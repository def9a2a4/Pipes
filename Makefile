build:
	gradle shadowJar
	mkdir -p bin
	cp build/libs/Pipes*.jar bin/

clean:
	gradle clean
	rm -rf bin/

server-plugin-copy:
	cp bin/Pipes*.jar server/plugins/

server-clear-plugin-data:
	rm -rf server/plugins/Pipes/

server-start:
	cd server && java -Xmx2G -jar paper-1.21.11-55.jar --nogui

server: build server-plugin-copy server-start

all: build
