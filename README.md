# LabelTyrannosaurus

LabelHub monorepo.

Backend AI stack: Spring AI Alibaba.
Object storage: Tencent Cloud COS.

## Layout

```text
frontend/   # frontend shell only
backend/    # standalone Spring Boot backend project
docs/       # design and contract docs
```

## Backend API Docs

Run the backend with the `local` or `dev` profile, then open `http://localhost:8080/doc.html` for the Knife4j OpenAPI UI. The Knife4j page supports exporting API documentation directly from the browser.
