.PHONY: build
build:
	gradle shadowJar
	mkdir -p bin
	cp build/libs/Pipes*.jar bin/

.PHONY: clean
clean:
	gradle clean
	rm -rf bin/

.PHONY: server-plugin-copy
server-plugin-copy:
	cp bin/Pipes*.jar server/plugins/

.PHONY: server-clear-plugin-data
server-clear-plugin-data:
	rm -rf server/plugins/Pipes/

.PHONY: server-start
server-start:
	cd server && java -Xmx2G -jar paper-1.21.11-55.jar --nogui

.PHONY: server
server: build server-plugin-copy server-start

.PHONY: all
all: clean build server
