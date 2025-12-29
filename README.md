# Tapir Akka-Http

<desc>Advanced back-end service with Tapir, Akka-Http, Quill and ZIO</desc>

Task - create service, similar to some kind of shop using Tapir with 
Akka-Http interpreter, Quill, ZIO and circe json. Also, show how to integrate ZIO into such service

This example contains:

- work with database using Quill 
- Simple jwt authentication and role authorization to access some endpoints
- sign in and sign out endpoints
- error handling customization
- failed decoding customization
- request wrappers (request handling time tracker)
- adding prometheus metrics
- connecting swagger docs
- set and describe http codes for error response
- Tapir endpoints testing, including mocking security
- Integrating ZIO into code - configuring services, making layers and executing them at the edges of the app

Also example contains Quill as library which works with database, similar to Slick

Note: to run this example in first time, you have to apply sql scripts from resource directory!


Stack
-
- Tapir v1.13.3
- Akka-Http
- ZIO Quill v4.8.5
- Circe json 0.14.15
- ZIO 2.1.24
- Scalatest, Mockito (testing)

Useful links
-
- Quill documentation: https://zio.dev/zio-quill/
- Tapir documentation: https://tapir.softwaremill.com/en/latest/index.html
- ZIO documentation https://zio.dev/overview/getting-started
- Circe documentation: https://circe.github.io/circe/
- Measuring response time in akka http (took implementation from there): https://blog.softwaremill.com/measuring-response-time-in-akka-http-7b6312ec70cf
- Tapir endpoints testing: https://tapir.softwaremill.com/en/latest/testing.html
