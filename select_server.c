#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <unistd.h>
#include <sys/socket.h>
#include <netinet/in.h>
#include <arpa/inet.h>
#include "httpd.h"

#define BUFFER_SIZE 2048
#define DEFAULT_PORT 8080

int main(int argc, char *argv[])
{
    struct sockaddr_in server_addr, client_addr;
    int client_addr_size;
    int listen_fd, conn_fd, max_fd;
    int client_fds[FD_SETSIZE];
    int index, max_index;
    fd_set read_set, all_set;
    int ready_fd_num;
    int on = 1;
    
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

    for (int i = 0; i < FD_SETSIZE; i++) {
        client_fds[i] = -1;
    }
    max_index = 0;

    FD_ZERO(&read_set);
    FD_ZERO(&all_set);
    FD_SET(listen_fd, &all_set);
    max_fd = listen_fd;

    while (1) {
        read_set = all_set;
        ready_fd_num = select(max_fd + 1, &read_set, NULL, NULL, NULL);
        if (ready_fd_num < 0) {
            perror("select error, message: ");
            continue;
        }

        if (FD_ISSET(listen_fd, &read_set)) {
            conn_fd = accept(listen_fd, (struct sockaddr *)&client_addr, &client_addr_size);
            if (conn_fd == -1) {
                perror("accept error, message: ");
                continue;
            }

            for (index = 1; index < FD_SETSIZE; index++) {
                if (client_fds[index] < 0) {
                    client_fds[index] = conn_fd;
                    break;
                }
            }

            if (index == FD_SETSIZE) {
                fprintf(stderr, "too many connections\n");
                close(conn_fd);
                continue;
            }

            if (index > max_index) {
                max_index = index;
            }

            FD_SET(conn_fd, &all_set);
            if (conn_fd > max_fd) {
                max_fd = conn_fd;
            }

            if (--ready_fd_num <= 0) {
                continue;
            }
        }

        for (int i = 1; i <= max_index; i++) {
            conn_fd = client_fds[i];
            if (conn_fd == -1) {
                continue;
            }

            if (FD_ISSET(conn_fd, &read_set)) {
                accept_request(conn_fd, &client_addr);
                FD_CLR(conn_fd, &all_set);
                close(conn_fd);
                client_fds[i] = -1;
            }

            if (--ready_fd_num <= 0) {
                break;
            }
        }
    }

    return 0;
}