all: $(patsubst %.java, %.class, $(wildcard *.java))

%.class: %.java
	javac $^

.PHONY: clean
clean:
	rm -f *.class

.PHONY: release
release:
	tar -zcvf dftp.tar.gz *.java