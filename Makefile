all: $(patsubst %.java, %.class, $(wildcard *.java))

%.class: %.java
	javac $^

.PHONY: run
run:
	java Test

.PHONY: clean
clean:
	rm -f *.class *.tar.gz

.PHONY: release
release:
	tar -zcvf dftp.tar.gz *.java
