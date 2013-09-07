# native2asciiplug

Intellij IDEA plugin which converts property files with native-encoded characters (characters which are non-Latin 1
and non-Unicode) to Unicode-encoded. Processing is being done automatically during compilation phase.

Available through Intellij IDEA Plugin Repository ([link](http://plugins.jetbrains.com/plugin/?id=6155)).

## Development

```sh
git clone https://github.com/shyiko/native2asciiplug.git
cd native2asciiplug
# update gradle.properties according to your environment
./gradlew clean zip # creates a zip file in build/distributions
```

## License

[Apache License, Version 2.0](http://www.apache.org/licenses/LICENSE-2.0)