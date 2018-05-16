# onyx-too-many-poll-invocations

Invoke:

    $ lein run > out.txt

then wait for the file to get populated. It will contain thousands of print statements
coming from `poll!`, even thogh only few are expected. Once in a while it will also have
print statements indicating clearly that `completed?` was returning `true` almost all the time.