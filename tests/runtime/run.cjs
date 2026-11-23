'use strict'

const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const { spawn } = require('node:child_process')
const { createHash } = require('node:crypto')
const mineflayer = require('mineflayer')
const mysql = require('mysql2/promise')

const version = process.env.MC_VERSION
const root = path.resolve('runtime-server')
const results = { version, server: 'Paper', client: '1.21.10', protocolTranslation: version !== '1.21.10', storage: 'MySQL 8.4', checks: [] }
const delay = ms => new Promise(resolve => setTimeout(resolve, ms))
let server, serverText = '', round = 0, probeId = 0, alice, bob
const clients = []
const pool = mysql.createPool({ host: '127.0.0.1', user: 'lobby', password: 'lobby_ci', database: 'lobby_runtime', connectionLimit: 4 })

function pass(name) { results.checks.push(name); console.log('INGAME PASS ' + name) }
async function until(test, name, timeout = 15000) {
  const end = Date.now() + timeout
  while (Date.now() < end) {
    if (await test()) return
    if (server && server.exitCode !== null) throw new Error('Server exited before ' + name)
    await delay(100)
  }
  throw new Error('Timeout: ' + name + '\n' + serverText.slice(-5000))
}
async function json(url) {
  const response = await fetch(url, { headers: { 'User-Agent': 'KekGames-LobbySystem-CI/1.1.0 (https://github.com/conlog06/lobbysystem-kekgames)' } })
  if (!response.ok) throw new Error(`${url}: HTTP ${response.status}`)
  return response.json()
}
async function download(url, destination, digest) {
  const response = await fetch(url)
  if (!response.ok) throw new Error(`Download HTTP ${response.status}`)
  const bytes = Buffer.from(await response.arrayBuffer())
  if (digest) assert.equal(createHash('sha256').update(bytes).digest('hex'), digest)
  fs.writeFileSync(destination, bytes)
}
async function prepareDownloads() {
  const builds = await json(`https://fill.papermc.io/v3/projects/paper/versions/${version}/builds`)
  const stable = builds.find(build => build.channel === 'STABLE')
  assert(stable, 'A stable Paper build must exist')
  const jar = stable.downloads['server:default']
  await download(jar.url, path.join(root, 'server.jar'), jar.checksums.sha256)
  results.paperBuild = stable.id
  if (version !== '1.21.10') {
    const dependencies = {
      ViaVersion: '24e4876fefe73e08a4c87cd6c457ee0916e62dd69656631caf41e26f909d504b',
      ViaBackwards: '9a148a80ea22074f4bd1626cdbe254a4cd998bf44e7ceaff4e285a1b6b35da42'
    }
    for (const [project, digest] of Object.entries(dependencies)) {
      const pinnedVersion = '5.12.2'
      await download(`https://github.com/ViaVersion/${project}/releases/download/${pinnedVersion}/${project}-${pinnedVersion}.jar`, path.join(root, 'plugins', project + '.jar'), digest)
      results[project] = pinnedVersion
    }
  }
}
async function startServer() {
  serverText = ''
  const output = fs.createWriteStream(path.join(root, `server-run-${++round}.log`))
  server = spawn('java', ['-Xms512M', '-Xmx1536M', '--enable-native-access=ALL-UNNAMED', '-jar', 'server.jar', '--nogui'], { cwd: root, stdio: ['pipe', 'pipe', 'pipe'] })
  for (const stream of [server.stdout, server.stderr]) stream.on('data', data => { output.write(data); serverText = (serverText + data.toString()).slice(-500000) })
  server.once('exit', () => output.end())
  await until(() => serverText.includes('RUNTIME READY') && /Done \(/.test(serverText), 'server and licensed plugin ready', 180000)
  consoleCommand('whitelist off')
  await delay(200)
}
function consoleCommand(command) { server.stdin.write(command + '\n') }
async function stopServer() {
  if (!server || server.exitCode !== null) return
  const process = server
  consoleCommand('stop')
  await new Promise((resolve, reject) => {
    const timeout = setTimeout(() => { process.kill('SIGTERM'); reject(new Error('Server did not stop cleanly')) }, 45000)
    process.once('exit', code => { clearTimeout(timeout); code === 0 ? resolve() : reject(new Error('Server exit ' + code)) })
  })
}
async function connect(username) {
  const bot = mineflayer.createBot({ host: '127.0.0.1', port: 25565, username, auth: 'offline', version: '1.21.10', hideErrors: false })
  bot.messages = []
  bot.lastChatAt = 0
  bot.on('messagestr', message => bot.messages.push(message))
  bot.on('error', error => { bot.runtimeError = error; console.error(username + ': ' + error.message) })
  bot.on('kicked', reason => { bot.runtimeError = new Error(JSON.stringify(reason)) })
  clients.push(bot)
  await new Promise((resolve, reject) => {
    const timeout = setTimeout(() => reject(new Error(username + ' spawn timeout')), 45000)
    bot.once('spawn', () => { clearTimeout(timeout); resolve() })
    bot.once('error', error => { clearTimeout(timeout); reject(error) })
    bot.once('kicked', reason => { clearTimeout(timeout); reject(new Error(JSON.stringify(reason))) })
  })
  await until(() => bot.inventory.slots[36]?.name === 'compass', username + ' hotbar initialized')
  return bot
}
async function chat(bot, command) {
  if (bot.runtimeError) throw bot.runtimeError
  await delay(Math.max(0, 450 - (Date.now() - bot.lastChatAt)))
  bot.lastChatAt = Date.now()
  bot.chat(command)
}
async function open(bot, command) {
  if (bot.currentWindow) { await bot.closeWindow(bot.currentWindow); await delay(150) }
  await chat(bot, command)
  await until(() => bot.currentWindow !== null, command + ' opens menu')
  await delay(150)
  return bot.currentWindow
}
async function click(bot, slot, mode = 0) {
  await Promise.race([bot.clickWindow(slot, 0, mode), delay(5000).then(() => { throw new Error('Menu click timeout') })])
  await delay(200)
}
async function account(name) {
  const [rows] = await pool.execute('SELECT * FROM lobby_players WHERE name=?', [name])
  assert.equal(rows.length, 1)
  return rows[0]
}
async function field(bot, key, expected) {
  await until(async () => String((await account(bot.username))[key]) === String(expected), bot.username + '.' + key + '=' + expected)
}
async function probe(test, bot) {
  const id = String(++probeId)
  consoleCommand(`runtimeprobe ${id} ${test}${bot ? ' ' + bot.username : ''}`)
  await until(() => {
    if (serverText.includes('RUNTIME FAIL ' + id + ' ')) throw new Error(serverText.slice(-6000))
    return serverText.includes('RUNTIME PASS ' + id + ' ')
  }, 'server probe ' + test)
  pass('server-' + test + (bot ? '-' + bot.username : ''))
}
async function disconnectAll() {
  for (const bot of clients) if (!bot._client.ended) bot.quit()
  clients.length = 0
  await delay(1000)
}

async function tests() {
  await startServer()
  await probe('ready')
  alice = await connect('BotAlice')
  bob = await connect('BotBob')
  consoleCommand('op BotAlice')
  await delay(700)
  await probe('session', alice)
  await probe('locked', alice)
  await until(() => alice.messages.some(message => message.includes('RUNTIME_WELCOME BotAlice')), 'welcome chat delivered')
  pass('welcome-delivered-to-client')

  consoleCommand('tp BotAlice 3.5 65 2.5')
  await until(() => alice.entity.position.x > 3, 'move into NPC interaction reach')
  await delay(400)
  await probe('reach', alice)
  for (const [entityName, expectedSlot, expectedItem] of [['witch', 11, 'paper'], ['villager', 10, 'ender_pearl']]) {
    await until(() => Object.values(alice.entities).some(entity => entity.name === entityName), entityName + ' visible to client')
    const npc = Object.values(alice.entities).find(entity => entity.name === entityName)
    console.log('NPC CLIENT TARGET', entityName, npc.id, npc.position, 'PLAYER', alice.entity.position)
    await alice.lookAt(npc.position.offset(0, 1, 0), true)
    await alice.activateEntityAt(npc, npc.position.offset(0, 1, 0))
    await alice.activateEntity(npc)
    await until(() => alice.currentWindow?.slots[expectedSlot]?.name === expectedItem, entityName + ' interaction opens menu')
    assert.equal(alice.currentWindow.slots[expectedSlot].name, expectedItem)
    await alice.closeWindow(alice.currentWindow)
    await delay(250)
    pass('real-NPC-interaction-' + entityName)
  }
  consoleCommand('tp BotAlice 0.5 65 0.5')
  await until(() => alice.entity.position.x < 2, 'return to spawn after NPC checks')

  let window = await open(alice, '/navigator')
  assert.equal(window.slots[14].name, 'diamond_pickaxe')
  await click(alice, 14, 1)
  assert.equal(alice.currentWindow.slots[14].name, 'diamond_pickaxe')
  assert.equal(alice.inventory.slots[36].name, 'compass')
  pass('navigator-shift-click-cannot-steal-items')
  await click(alice, 14, 2)
  assert.equal(alice.inventory.slots[36].name, 'compass')
  pass('navigator-number-click-preserves-hotbar')
  await alice.closeWindow(alice.currentWindow)
  await chat(alice, '/setwarp skyblock')
  await delay(300)
  consoleCommand('tp BotAlice 4.5 65 4.5')
  await until(() => alice.entity.position.x > 4, 'move away from warp')
  await open(alice, '/navigator'); await click(alice, 14)
  await until(() => alice.entity.position.x < 2, 'navigator teleports to configured warp')
  pass('navigator-local-warp')

  await open(alice, '/reward'); await click(alice, 11)
  await field(alice, 'coins', 200); await field(alice, 'tickets', 1)
  pass('normal-daily-reward')
  await open(alice, '/reward'); await click(alice, 11)
  await field(alice, 'coins', 200)
  assert(alice.messages.some(message => message.includes('bereits abgeholt')))
  pass('duplicate-daily-reward-rejected')
  await open(alice, '/reward'); await click(alice, 15)
  await field(alice, 'coins', 600); await field(alice, 'tickets', 3)
  pass('premium-daily-reward')
  await chat(alice, '/lobbycoins add BotAlice 5000'); await field(alice, 'coins', 5600)
  await chat(alice, '/pay BotBob 250'); await field(alice, 'coins', 5350); await field(bob, 'coins', 250)
  pass('ingame-pay-to-second-connected-player')
  await chat(bob, '/pay BotAlice 999999'); await field(bob, 'coins', 250)
  await until(() => bob.messages.some(message => message.includes('Überweisung fehlgeschlagen')), 'insufficient funds message')
  pass('ingame-pay-insufficient-funds')

  const beforeLottery = Number((await account(alice.username)).coins)
  await open(alice, '/lottery'); await click(alice, 7)
  await field(alice, 'tickets', 2)
  const prize = Number((await account(alice.username)).coins) - beforeLottery
  assert([25, 50, 100, 200, 500, 1000].includes(prize))
  pass('lottery-consumes-ticket-and-awards-configured-prize')

  for (const [category, column, value] of [['particles', 'particle', 'SNOWFLAKE'], ['balloons', 'balloon', 'LIME_CONCRETE'], ['gadgets', 'gadget', 'DASH'], ['heads', 'head', 'CREEPER_HEAD']]) {
    await open(alice, '/cosmetics')
    const slot = { particles: 11, balloons: 13, gadgets: 15, heads: 22 }[category]
    await click(alice, slot)
    await until(() => alice.currentWindow && alice.currentWindow.slots[12]?.name !== 'gray_stained_glass_pane', category + ' submenu')
    await click(alice, 12)
    await field(alice, column, value)
    pass('cosmetic-purchase-' + category)
  }
  const bought = Number((await account(alice.username)).coins)
  await open(alice, '/cosmetics'); await click(alice, 11); await delay(200); await click(alice, 12)
  await field(alice, 'coins', bought)
  pass('owned-cosmetic-selection-does-not-charge-again')
  await probe('cosmetics', alice)
  alice.setQuickBarSlot(1); alice.activateItem(); await delay(150); await probe('gadget', alice)
  consoleCommand('tp BotAlice 0.5 65 0.5'); await delay(500)

  await open(alice, '/settings'); await click(alice, 10); await delay(200); await click(alice, 15)
  await field(alice, 'visibility', 2); await probe('hidden', alice)
  await open(alice, '/settings'); await click(alice, 10); await delay(200); await click(alice, 11)
  await field(alice, 'visibility', 0); await probe('visible', alice)
  await open(alice, '/settings'); await click(alice, 16)
  await field(alice, 'jump', 0)
  pass('double-jump-setting-saved')
  await open(alice, '/settings'); await click(alice, 16)
  await field(alice, 'jump', 1); await probe('jump', alice)

  await chat(alice, '/build'); await delay(300); await probe('build', alice)
  await chat(alice, '/build'); await delay(300); await probe('session', alice)
  const padVelocities = []
  const velocityListener = packet => { if (packet.entityId === alice.entity.id) padVelocities.push(packet) }
  alice._client.on('entity_velocity', velocityListener)
  consoleCommand('tp BotAlice 6.5 65 0.5')
  await until(() => alice.entity.position.x > 6 && alice.entity.position.x < 7, 'place player on launch pad')
  alice.setControlState('forward', true)
  try {
    await until(() => padVelocities.some(packet => (packet.velocity?.y ?? packet.velocityY ?? 0) > 0), 'launch pad delivers upward velocity packet to connected player')
  } finally {
    alice.setControlState('forward', false)
    console.log('LAUNCH CLIENT STATE', alice.entity.position, alice.entity.velocity, alice.game.gameMode, JSON.stringify(padVelocities))
  }
  alice._client.removeListener('entity_velocity', velocityListener)
  console.log('LAUNCH CLIENT VELOCITY', JSON.stringify(padVelocities))
  await probe('launch', alice)
  pass('launch-pad-delivers-upward-velocity-to-real-client')
  consoleCommand('tp BotAlice 0.5 65 0.5')
  window = await open(alice, '/profil')
  assert.equal(window.slots[12].name, 'gold_ingot')
  assert.equal(window.slots[14].name, 'clock')
  pass('profile-menu-client-inventory')
  await open(alice, '/top coins')
  await until(() => alice.currentWindow?.slots[10]?.name === 'gold_ingot', 'coins rankings loaded')
  await click(alice, 8)
  await until(() => alice.currentWindow?.slots[10]?.name === 'clock', 'playtime rankings loaded')
  pass('both-ranking-categories-client-inventory')
  await alice.closeWindow(alice.currentWindow)
  await until(() => alice.messages.some(message => message.includes('RUNTIME_ANNOUNCEMENT BotAlice')), 'announcement delivered', 20000)
  pass('personalized-announcement-delivered')
  await chat(alice, '/lobbylicense create Forbidden 1 00000000-0000-0000-0000-000000000000')
  await until(() => alice.messages.some(message => message.includes('Nur SujucDerBreite')), 'operator cannot issue licenses')
  pass('operator-cannot-create-owner-license')

  const saved = await account(alice.username)
  await disconnectAll(); await stopServer()
  await startServer(); await probe('ready')
  alice = await connect('BotAlice'); bob = await connect('BotBob')
  await field(alice, 'coins', saved.coins); await field(alice, 'head', 'CREEPER_HEAD'); await field(alice, 'particle', 'SNOWFLAKE')
  await probe('cosmetics', alice)
  pass('mysql-and-cosmetics-persist-after-real-server-restart')
  await open(alice, '/reward'); await click(alice, 11); await field(alice, 'coins', saved.coins)
  await until(() => alice.messages.some(message => message.includes('bereits abgeholt')), 'reward remains claimed after restart')
  pass('daily-claim-persists-after-server-restart')
  await probe('expire'); await delay(1600); await probe('inactive')
  await chat(alice, '/profil')
  await until(() => alice.messages.some(message => message.includes('Keine gültige Lizenz')), 'expired license rejects menus')
  pass('expired-license-blocks-real-player-command')
  await probe('activate'); await probe('ready'); await delay(500); await probe('session', alice)
  for (const bot of [alice, bob]) if (bot.runtimeError) throw bot.runtimeError
  assert(!/\[LobbySystem\].*(?:Start fehlgeschlagen|Datenbankoperation fehlgeschlagen)/.test(serverText), 'No plugin or database runtime errors')
  pass('no-plugin-or-database-runtime-errors')
}

;(async () => {
  try {
    await prepareDownloads()
    await tests()
    results.success = true
    console.log(`INGAME SUCCESS ${version}: ${results.checks.length} live checks`)
  } catch (error) {
    results.success = false; results.error = error.stack
    console.error(error.stack); console.error(serverText.slice(-12000)); process.exitCode = 1
  } finally {
    await disconnectAll().catch(() => {})
    await stopServer().catch(error => { console.error(error); process.exitCode = 1; results.success = false })
    await pool.end()
    fs.writeFileSync(path.join(root, 'runtime-results.json'), JSON.stringify(results, null, 2))
  }
})()
