package Moose::Exception::CreateTakesHashRefOfAttributes;
our $VERSION = '2.1404';

use Moose;
extends 'Moose::Exception';
with 'Moose::Exception::Role::RoleForCreate';

sub _build_message {
    "You must pass a HASH ref of attributes";
}

1;
