# LFTP

A large file transfer tool.

[[GitHub](https://github.com/MegaShow/LFTP)\]  [[Design Doc]()\]  [[Test Doc]()\]

## Installation

下载[lftp-1.0.0.jar](https://github.com/MegaShow/LFTP/releases/tag/v1.0.0)，并运行即可。

可通过PowerShell的别名来简化命令输入。

```powershell
function __lftp {java -jar C:\Your_LFTP_PATH\lftp-1.0.0.jar $args}
Set-Alias lftp __lftp
```

然后运行LFTP测试是否配置成功。

```sh
$ lftp
```

## Build

项目使用 [Gradle](https://gradle.org/)管理。

通过Gradle运行程序，`--args`指定提供给Jar的参数。

```sh
$ gradle run -q --args='console_parameters'
```

构造Jar文件，其中在`build/libs`文件夹中生成Jar，在`build/distributions`文件夹中生成Zip或Tar压缩包。压缩包中包含Jar和Windows、Linux的启动脚本。

```sh
$ gradle build
```

## Usage

```sh
$ lftp -h
Usage: lftp [-hV] [COMMAND]
A large file transfer tool.
  -h, --help      Show this help message and exit.
  -V, --version   Print version information and exit.
Commands:
  serve     Listen and serve at a port.
  lget, g   Download a file from server.
  lsend, s  Upload a file to server.
```

LFTP提供客户端和服务端，使用`serve`可以监听某一个端口，使本地服务端化。端口默认为`2333`。

```sh
$ lftp serve -h
Usage: lftp serve [-hV] [-p=port] folder
Listen and serve at a port.
      folder      Data folder for server.
  -h, --help      Show this help message and exit.
  -p=port         Port for server.
  -V, --version   Print version information and exit.
```

LFTP使用`lsend`和`lget`命令可以向服务端发送数据和从服务端拉取数据。

```sh
$ lftp lsend -h
Usage: lftp lsend [-hV] server_url file_name
Upload a file to server.
      server_url   Server's url or ip.
      file_name    Filename which need to upload.
  -h, --help       Show this help message and exit.
  -V, --version    Print version information and exit.
```

```sh
$ lftp lget -h
Usage: lftp lget [-hV] server_url file_name
Download a file from server.
      server_url   Server's url or ip.
      file_name    Filename which need to download.
  -h, --help       Show this help message and exit.
  -V, --version    Print version information and exit.
```

## License

None.

