clean:
	rm -rf *.class
compile-client:
	javac AudioClient.java
compile-server:
	javac AudioServer.java

server: compile-server
	java AudioServer
client: compile-client
	java AudioClient
