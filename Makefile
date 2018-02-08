all: single_process_server multiprocess_server select_server poll_server epoll_multiprocess_server
.PHONY: all

single_process_server: single_process_server.o httpd.o
	cc -o single_process_server single_process_server.o httpd.o

multiprocess_server: multiprocess_server.o httpd.o
	cc -o multiprocess_server multiprocess_server.o httpd.o

select_server: select_server.o httpd.o
	cc -o select_server select_server.o httpd.o

poll_server: poll_server.o httpd.o
	cc -o poll_server poll_server.o httpd.o

epoll_multiprocess_server: epoll_multiprocess_server.o httpd.o
	cc -o epoll_multiprocess_server epoll_multiprocess_server.o httpd.o

httpd.o: httpd.c
	cc -g -c httpd.c 

single_process_server.o: single_process_server.c httpd.h
	cc -g -c single_process_server.c

multiprocess_server.o: multiprocess_server.c httpd.h
	cc -c multiprocess_server.c

select_server.o: select_server.c httpd.h
	cc -c select_server.c

poll_server.o: poll_server.c httpd.h
	cc -c poll_server.c

epoll_multiprocess_server.o: epoll_multiprocess_server.c httpd.h
	cc -c epoll_multiprocess_server.c

clean:
	rm httpd.o single_process_server single_process_server.o \
		multiprocess_server multiprocess_server.o select_server.o select_server \
		poll_server poll_server.o epoll_multiprocess_server epoll_multiprocess_server.o
