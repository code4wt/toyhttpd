# toyhttpd
## 1.简介
I/O 模型练手代码，分别使用阻塞式 I/O、select、poll 和 epoll 和 Java NIO 实现了简单的 HTTP Server。

本仓库对应于 Java NIO 系列文章，文章地址为[http://www.coolblog.xyz/categories/foundation-of-java/NIO/](http://www.coolblog.xyz/categories/foundation-of-java/NIO/)

## 2.使用说明
### 2.1 C代码编译
C 代码可直接使用 make 命令编译，但因 epoll 相关接口是 Linux 系统特有的，所以应保证在 Linux 平台下编译。

### 2.2 
Java 代码并未依赖第三方 Jar 包，所以可直接使用 javac 命令编译。

```shell
cd java
javac xyz/coolblog/httpd/TinyHttpd.java
```