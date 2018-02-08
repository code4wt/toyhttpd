#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <unistd.h>
#include <sys/socket.h>
#include <netinet/in.h>
#include <sys/wait.h>
#include <signal.h>
#include "httpd.h"

#define BUFFER_SIZE 2048
#define DEFAULT_PORT 8080

void handle_subprocess_exit(int);

int main(int argc, char *argv[])
{
    struct sockaddr_in server_addr, client_addr;
    int client_addr_size;
    int listen_fd, conn_fd;
    int on;
    
    client_addr_size = sizeof(client_addr);
    listen_fd = socket(AF_INET, SOCK_STREAM, 0);
    setsockopt(listen_fd, SOL_SOCKET, SO_REUSEADDR, &on, sizeof(on));

    bzero(&server_addr, sizeof(server_addr));
    server_addr.sin_family = AF_INET;
    server_addr.sin_addr.s_addr = htonl(INADDR_ANY);
    server_addr.sin_port = htons(DEFAULT_PORT);

    if (bind(listen_fd, (struct sockaddr *)&server_addr, sizeof(server_addr)) == -1) {
        perror("bind error, message: ");
        exit(1);
    }

    if (listen(listen_fd, 5) == -1) {
        perror("listen error, message: ");
        exit(1);
    }

    printf("listening 8080\n");

    signal(SIGCHLD, handle_subprocess_exit);

    while (1) {
        conn_fd = accept(listen_fd, (struct sockaddr *)&client_addr, &client_addr_size);
        if (conn_fd == -1) {
            perror("accept error, message: ");
            continue;
        }

        pid_t pid = fork();
        if (pid == 0) {
            close(listen_fd);
            accept_request(conn_fd, &client_addr);
            close(conn_fd);
            exit(0);
        }

        close(conn_fd);
    }

    return 0;
}

void handle_subprocess_exit(int signo) {
    int status;
    while(waitpid(-1, &status, WNOHANG) > 0);
}