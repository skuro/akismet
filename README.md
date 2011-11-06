# akismet -- A Clojure library for the Akismet API

[Akismet](http://akismet.com/) filters out your comment and track-back spam for you, so you can focus on more important things.

This library provides a Clojure interface to use Akismet as part of your blog application. If you're using [Ring](https://github.com/mmcgrana/ring), a `wrap-akismet` middleware is provided to automagically submit requests to Akismet.

## Usage

A few functions are provided:

- `valid-key?` -- calls [Verify Key](http://akismet.com/development/api/#verify-key)

- `spam?` -- calls [Comment Check](http://akismet.com/development/api/#comment-check)

- `spam!` -- calls [Submit Spam](http://akismet.com/development/api/#submit-spam)

- `ham!` -- calls [Submit Ham](http://akismet.com/development/api/#submit-ham)

Sample use:

```clojure
    (binding [*akismet-key* "123123YourApiKey"
              *akismet-blog* "http://your.blog.here"]
      (spam? {:user_ip "127.0.0.1"
              :user_agent "foo bar"
              :referrer "http://google.com"
              :permalink "http://your.blog.here/1"
              :comment_type "comment"
              :comment_author "skuro"
              :comment_author_email "skuro@skuro.tk"
              :comment_author_url "http://skuro.tk"
              :comment_content "Free viagra!"}
```

## TODO

- Write some tests

- Fix the `wrap-akismet` middleware to extract meaningful parameters from the request before calling Akismet

- Move to an async HTTP client library to avoid busy waits on Akismet calls

## License

Copyright (C) 2011 Carlo Sciolla

Distributed under the Eclipse Public License, the same as Clojure.
