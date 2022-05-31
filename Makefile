CURRENT_DIR = $(shell pwd)

# Local un-containerized execution (make serve)
XTDB_PATH = ${CURRENT_DIR}/xtdb

.PHONY: clean
clean:
	rm -rf logs/* .clj-kondo/.cache/* ${XTDB_PATH}
	make -C setup clean

.PHONY: repl
repl:
	clj -J--add-opens=java.base/java.util.concurrent=ALL-UNNAMED --repl
