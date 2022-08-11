# Tapir Akka-Http

<desc>Advanced back-end service with Tapir, Akka-Http, Quill and Macwire</desc>

Task - create service, similar to some kind of shop using Tapir with 
Akka-Http interpreter, Quill, Macwire and circe json.

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

Also example contains Quill as library which works with database, similar to Slick and Macwire, which works similar to Guice.

Stack
-
- Tapir v1.0.3
- Akka-Http
- Quill v4.2.0
- Circe json 0.14.2
- Macwire v2.5.7
- Scalatest, Mockito (testing)

Useful links
-
- Quill documentation: https://getquill.io/
- Tapir documentation: https://tapir.softwaremill.com/en/latest/index.html
- Macwire github: https://github.com/softwaremill/macwire
- Circe documentation: https://circe.github.io/circe/
- Measuring response time in akka http (took implementation from there): https://blog.softwaremill.com/measuring-response-time-in-akka-http-7b6312ec70cf
