
# http-server-v2

Minimal Java HTTP server built **only** with `java.net`, `java.io`, and `java.nio`.

This project is intentionally **not** a framework, **not** RFC-complete, and **not** production-safe.  
Its purpose is to **show what raw HTTP actually looks like on the wire** and how a server can be structured without hiding behavior behind libraries.

---

## Goals

- Show raw HTTP request/response structure
- Save full incoming requests verbatim to disk
- Separate concerns cleanly without frameworks
- Demonstrate:
  - Static GET handling with MIME types
  - Simple POST and multipart POST handling
  - Config-driven dynamic GET handlers
- Keep everything readable in one sitting

---

## Deliberately naive parsing (important)

Parsing is **intentionally naive**.

Examples:
- Headers are not fully parsed
- Content-Length is not enforced
- Chunked encoding is ignored
- Multipart parsing is string-based
- Binary files are not decoded properly

This is **by design**.

The goal is to let a developer:
- Open `out/http_*.txt`
- See exactly what the browser sent
- Correlate browser behavior with bytes on disk
- Understand where real HTTP servers add complexity

If this were “correct”, most of the learning value would disappear.

---

## Project structure

```

src/main/java/demo
├── SimpleHttpServer.java      # Socket accept + routing
├── StaticGetHandler.java      # GET static files, MIME handling
├── RequestSaver.java          # Saves raw HTTP requests
├── MultipartPostHandler.java  # Very naive multipart handling
└── Extn.java                  # User-defined GET extensions

www/
├── index.html
├── form1.html                 # Simple POST
└── form2.html                 # Multipart POST (2 files)

out/
└── http_<timestamp>_NN.txt    # Raw requests (auto-created)

```

---

## Configuration

`src/main/resources/application.properties`

```

port=8080
out.dir=./out
[www.dir=./www](http://www.dir=./www)
handler1=/h1

```

Dynamic handlers are mapped by convention:

```

handler1=/h1  ->  Extn.handle1(...)
handler2=/x   ->  Extn.handle2(...)

````

Required method signature:

```java
public void handle1(Map<String,String> getParams, OutputStream out)
````

---

## Example dynamic handler

Request:

```
GET /h1?a1=2.5&a2=3.5 HTTP/1.1
```

Handler logic:

* Reads `a1`, `a2`
* Parses to `double`
* Returns sum
* On error returns `error`

This is intentionally primitive to keep reflection and routing visible.

---

## Running

```
mvn compile
java -cp target/classes demo.SimpleHttpServer
```

Open:

* [http://localhost:8080/](http://localhost:8080/)
* [http://localhost:8080/form1.html](http://localhost:8080/form1.html)
* [http://localhost:8080/form2.html](http://localhost:8080/form2.html)
* [http://localhost:8080/h1?a1=1.2&a2=3.4](http://localhost:8080/h1?a1=1.2&a2=3.4)

---

## How real servers do this (better parsing)

This project avoids doing the following - **on purpose**.

### 1. Proper request line + header parsing

Real servers:

* Read until CRLF
* Parse request line into method, path, version
* Parse headers into a case-insensitive map
* Respect `Content-Length` and `Transfer-Encoding`

Example (simplified):

```java
BufferedReader br = new BufferedReader(
    new InputStreamReader(in, StandardCharsets.ISO_8859_1));

String requestLine = br.readLine();
Map<String,String> headers = new HashMap<>();

String line;
while (!(line = br.readLine()).isEmpty()) {
    int idx = line.indexOf(':');
    headers.put(
        line.substring(0, idx).trim().toLowerCase(),
        line.substring(idx + 1).trim()
    );
}
```

---

### 2. Correct multipart parsing

Real multipart parsing:

* Uses boundary markers
* Streams file content (not string-based)
* Handles binary safely
* Respects `Content-Disposition`, `Content-Type`

Example approach (not full):

```java
String boundary = extractBoundary(headers.get("content-type"));
InputStream body = new LimitedInputStream(in, contentLength);

MultipartStream ms = new MultipartStream(body, boundaryBytes);
```

This is why libraries exist.

---

### 3. Robust GET parameter decoding

Correct decoding requires:

* URL decoding
* Character encoding awareness
* Duplicate key handling

Example:

```java
String decoded = URLDecoder.decode(value, StandardCharsets.UTF_8);
```

---

### 4. Why frameworks exist

Frameworks like:

* Jetty
* Tomcat
* Netty
* Undertow
* Spring Web

exist because HTTP edge cases are endless:

* Slow clients
* Partial reads
* Chunked encoding
* Pipelining
* Security attacks

This project intentionally ignores all of that.

---

## When to use this code

Use this project when you want to:

* Teach HTTP from first principles
* Debug weird client behavior
* Understand what frameworks hide
* Build intuition before using libraries

Do **not** use it for:

* Production
* Internet-facing servers
* Security-sensitive systems

---

## Version Control

The project includes a `.gitignore` file that excludes:

* **Build artifacts**: `target/` directory (Maven compiled classes, JARs)
* **Server output**: `out/` directory (saved HTTP requests)
* **Temporary files**: `cp.txt`, `*.log`
* **IDE files**: `.idea/`, `*.iml`, `.vscode/`, etc.

These files are generated during runtime and should not be committed to version control.

To set up version control properly:

```bash
# Create .gitignore (already included)
# Remove any accidentally staged files
git reset

# Re-add files with .gitignore applied
git add .

# Verify only source files are staged
git status
```

Only source code, configuration files, and documentation should be tracked in Git.

---

## License / intent

Educational, exploratory, throwaway code.

If you understand this project, frameworks will feel simpler - not magical.

---

## Important Requirements

### Encoding

**All source files (HTML, CSS, JS, and Java) in both project versions (v1 and v2) must use UTF-8 encoding.**

Both `pom.xml` files include:
```xml
<project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
```

### GET Request Failure Handling

If a GET request fails to find/parse text after a **third attempt**, a trigger file is created in the output directory to initiate a directory listing operation (`ls -al`).

The listing output is formatted using `<pre>` tags for simple, monospaced display.

### Local Development Only

> [!CAUTION]
> The file listing functionality and debugging features are **strictly for local development** and should **NOT** be used in production environments.

This server is designed for educational purposes and local debugging only. Do not expose it to the internet or use it in production systems.

