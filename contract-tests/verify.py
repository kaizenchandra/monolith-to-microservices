"""Executable exact consumer contract, stdlib only; used by E2E across both providers."""
import json
import pathlib


def product(value):
    contract = json.loads((pathlib.Path(__file__).parent / "product-v1.json").read_text())
    assert set(value) == set(contract["required"])
    for key, schema in contract["properties"].items():
        v = value[key];
        typ = {"integer": int, "string": str, "boolean": bool}[schema["type"]]
        assert type(v) is typ, (key, v)
        if "minimum" in schema: assert v >= schema["minimum"]
    return value
