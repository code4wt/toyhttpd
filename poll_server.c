#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <unistd.h>
#include <sys/socket.h>
#include <netinet/in.h>
#include <arpa/inet.h>
#include <poll.h>
#include "httpd.h"

#define BUFFER_SIZE 2048
#define DEFAULT_PORT 8080
#define INFTIM -1
#define OPEN_MAX 1024

int main(int argc, char *argv[])
{
    struct sockaddr_in server_addr, client_addr;
    int client_addr_size;
    int listen_fd, conn_fd;
    struct pollfd client_fds[OPEN_MAX];
    int index, max_index;
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

    for (int i = 0; i < OPEN_MAX; i++) {
        client_fds[i].fd = -1;
    }
    max_index = 0;

    client_fds[0].fd = listen_fd;
    client_fds[0].events = POLLRDNORM;

    while (1) {
        ready_fd_num = poll(client_fds, max_index + 1, INFTIM);
        if (ready_fd_num < 0) {
            perror("poll error, message: ");
            continue;
        }

        if (client_fds[0].revents & POLLRDNORM) {
            conn_fd = accept(listen_fd, (struct sockaddr *)&client_addr, &client_addr_size);
            if (conn_fd == -1) {
                perror("accept error, message: ");
                continue;
            }

            for (index = 1; index < OPEN_MAX; index++) {
                if (client_fds[index].fd < 0) {
                    client_fds[index].fd = conn_fd;
                    client_fds[index].events = POLLRDNORM;
                    break;
                }
            }

            if (index == OPEN_MAX) {
                fprintf(stderr, "too many connections\n");
                close(conn_fd);
                continue;
            }

            if (index > max_index) {
                max_index = index;
            }

            if (--ready_fd_num <= 0) {
                continue;
            }
        }

        for (int i = 1; i <= max_index; i++) {
            if ((conn_fd = client_fds[i].fd) < 0) {
                continue;
            }

            if (client_fds[i].revents & POLLRDNORM) {
                accept_request(client_fds[i].fd, &client_addr);
                close(client_fds[i].fd);
                client_fds[i].fd = -1;
            } else {
                close(client_fds[i].fd);
                client_fds[i].fd = -1;
            }

            if (--ready_fd_num <= 0) {
                break;
            }
        }
    }

    return 0;
}