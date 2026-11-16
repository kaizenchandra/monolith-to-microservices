.PHONY: verify up bootstrap demo failures postflight benchmark stop
verify:
	mvn verify
up:
	docker compose up -d --build
bootstrap:
	python3 integration-tests/bootstrap.py
demo:
	python3 integration-tests/demo.py
failures:
	python3 integration-tests/failures.py
postflight:
	python3 integration-tests/postflight.py
benchmark:
	python3 benchmarks/run.py
stop:
	docker compose stop
