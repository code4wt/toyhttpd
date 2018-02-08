#ifndef HTTPD_H
#define HTTPD_H

void accept_request(int, struct sockaddr_in *);
void bad_request(int);
void cat(int, FILE *);
void cannot_execute(int);
void error_die(const char *);
void execute_cgi(int, const char *, const char *, const char *);
int get_line(int, char *, int);
void headers(int, const char *);
void suffix2type(const char *, char *);
int is_text_type(const char *);
void get_file_suffix(const char *, char *);
void not_found(int);
void serve_file(int, const char *);
int startup(u_short *);
void unimplemented(int);
char* get_time_str(char[]);

#endif