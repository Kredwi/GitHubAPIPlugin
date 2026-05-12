# GitHubAPIPlugin

## Description

The plugin allows binding a GitHub account to a player and displaying information about it via **PlaceholdersAPI**.

## Requirements for operation

1. **Java 8+** or higher
2. **Spigot** / **Paper** from version 1.12 up to the latest
3. Installed **PlaceholdersAPI** plugin version **2.12.2** or newer

## How it works

1. The player binds their GitHub nickname with the command `/githubapi link <username>`.
2. The plugin saves the connection in MySQL and creates a session to retrieve data from the GitHub API upon player
   login.
3. Data is updated only when the player re-enters the server.
4. An administrator displays information via the plugin's placeholders (PlaceholdersAPI required).

> [!IMPORTANT]
> The `/githubapi link` command makes a direct HTTP request to GitHub. It is recommended to set a cooldown to prevent
> API blocking.

### Dependencies

- **PlaceholdersAPI** – mandatory plugin for displaying information. Without it, placeholders will not work.

#### Internal Dependencies
- HikariCP
- SL4J
- BStats

## Placeholders

All placeholders follow the format `%githubapi_<parameter>%`.  
Examples: `%githubapi_login%`, `%githubapi_name%`, `%githubapi_bio%`.

### Supported parameters

| Parameter          | Description                   |
|--------------------|-------------------------------|
| `login`            | User login                    |
| `id`               | User ID                       |
| `type`             | User type                     |
| `name`             | User name                     |
| `company`          | Company                       |
| `blog`             | Blog/website link             |
| `location`         | Location                      |
| `email`            | Email address                 |
| `hireable`         | Available for hire            |
| `bio`              | Biography                     |
| `followers`        | Number of followers           |
| `following`        | Number of following           |
| `node_id`          | GraphQL node ID               |
| `twitter_username` | Twitter username              |
| `created_at`       | Account creation date         |
| `updated_at`       | Last update date              |
| `public_repos`     | Number of public repositories |
| `public_gists`     | Number of public Gists        |
| `site_admin`       | Is site admin                 |
| `user_view_type`   | User display type             |

## Installation (for server administrator)

1. Download the ready JAR file from the [Releases](https://github.com/Kredwi/GitHubAPIPlugin/releases) section.
2. Place the file into your server’s `plugins` folder.
3. Restart the server.
4. Configure the MySQL connection in `config.yml` (see the "Configuration" section).
5. Grant players the necessary permissions (see the "Permissions" section).

## Configuration

The `config.yml` file is created automatically on first run. It contains:

- Database connection settings (host, port, database name, user, password)
- All messages visible to players (translatable strings)

> [!WARNING]
> After modifying `config.yml`, you must **fully restart the server**.

## Database

- Only MySQL and SQLite are supported
- Queries are executed asynchronously using a pool of 5 threads.
- When unloading the plugin, the thread pool is properly shut down.

### Thread pool

The thread pool is limited to 5 threads:

```java
private final ExecutorService dbExecutorService = Executors.newFixedThreadPool(5);
```

## Commands

The main server command is `/githubapi`. Subcommands:

| Command                         | Description                   |
|---------------------------------|-------------------------------|
| `/githubapi link <gitUsername>` | Bind a GitHub account         |
| `/githubapi show`               | Show the bound GitHub account |

## Permissions

| Permission               | Description                             |
|--------------------------|-----------------------------------------|
| `githubapi.command`      | Access to the main `/githubapi` command |
| `githubapi.command.link` | Access to the `link` subcommand         |
| `githubapi.command.show` | Access to the `show` subcommand         |

## Building from source (for developers)

If you want to build the plugin yourself:

### Tool setup

1. Install **Java 8** or newer
2. Install **Maven** (instructions: [maven.apache.org](https://maven.apache.org/install.html))

### Build

```bash
git clone https://github.com/Kredwi/GitHubAPIPlugin
cd GitHubAPIPlugin
mvn package
```

The compiled JAR will be located in the `target/` folder with the name `GithubAPIPlugin-<VERSION>-SNAPSHOT.jar`.

## GitHub API limitations

| Request type              | Limit per hour | Token required |
|---------------------------|----------------|----------------|
| Without token (server IP) | 60             | ❌              |
| With token                | 5000           | ✅              |

If the limit is exceeded, the plugin will return empty values until the next hour.

## Useful links

- [Configuration file (config.yml)](https://github.com/Kredwi/GitHubAPIPlugin/blob/master/src/main/resources/config.yml)
- [GitHub REST API documentation (users)](https://docs.github.com/en/rest/users/users)

## License

This project is licensed under the **Apache License 2.0**.  
More details: [LICENSE](https://github.com/Kredwi/GitHubAPIPlugin/blob/master/LICENSE)
