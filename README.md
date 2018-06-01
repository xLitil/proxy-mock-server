# proxy-mock-sever

1. Download proxy-mock-server JAR
2. Run JAR `java -jar target\proxy-mock-server-1.0.0-SNAPSHOT-jar-with-dependencies.jar --port 8888`
3. Configure you application to pass through the proxy-mock-server

Based on [mock-server](http://www.mock-server.com/mock_server/getting_started.html)

## Matching

```json
{
  "httpRequest" : {
    ...
    "body" : {
      "type" : "STRING",
      "string" : "restituerListeComptes",
      "subString": true
    }
  }
  ...
```
```json
{
  "httpRequest" : {
    ...
    "body" : {
      "type" : "REGEX",
      "regex" : ".*restituerListeComptes.*"
    }
  }
  ...
```
```json
{
  "httpRequest" : {
    ...
    "body" : {
      "type" : "STRING",
      "string" : "...exact string...",
      "contentType" : "text/plain; charset=utf-8"
    }
  }
  ...
```
