# Third-party components

Offline build runtime inputs:

| Component | Version | Upstream | License |
|---|---|---|---|
| Gson | 2.14.0 | https://github.com/google/gson | Apache-2.0 |
| SQLite JDBC | 3.49.1.0 | https://github.com/xerial/sqlite-jdbc | Apache-2.0 / bundled native notices |
| MySQL Connector/J | 9.2.0 | https://github.com/mysql/mysql-connector-j | GPL-2.0 with Universal FOSS Exception |
| Protobuf Java | 4.29.0 | https://github.com/protocolbuffers/protobuf | BSD-3-Clause |
| SLF4J API | 2.0.18 | https://www.slf4j.org | MIT |

The public source repository does not vendor binary dependencies. Maven resolves the versions in pom.xml; the optional offline builder uses locally installed server libraries, whose versions are listed above for the verified local build. The original 1.0.0 Owner ZIP includes the unmodified dependency JARs and their notices under third-party/. The fat JAR merges service registrations and removes signature files invalidated by repackaging. No Spigot/Paper server code or API classes are bundled in the plugin.
