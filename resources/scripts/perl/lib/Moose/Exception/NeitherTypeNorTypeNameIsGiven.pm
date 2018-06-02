package Moose::Exception::NeitherTypeNorTypeNameIsGiven;
our $VERSION = '2.1404';

use Moose;
extends 'Moose::Exception';

sub _build_message {
    "You need to give type or type_name or both";
}

1;
