package Sub::Name; # git description: v0.13-7-g79187d2
# ABSTRACT: (re)name a sub

#pod =pod
#pod
#pod =head1 SYNOPSIS
#pod
#pod     use Sub::Name;
#pod
#pod     subname $name, $subref;
#pod
#pod     $subref = subname foo => sub { ... };
#pod
#pod =head1 DESCRIPTION
#pod
#pod This module has only one function, which is also exported by default:
#pod
#pod =for stopwords subname
#pod
#pod =head2 subname NAME, CODEREF
#pod
#pod Assigns a new name to referenced sub.  If package specification is omitted in
#pod the name, then the current package is used.  The return value is the sub.
#pod
#pod The name is only used for informative routines (caller, Carp, etc).  You won't
#pod be able to actually invoke the sub by the given name.  To allow that, you need
#pod to do glob-assignment yourself.
#pod
#pod Note that for anonymous closures (subs that reference lexicals declared outside
#pod the sub itself) you can name each instance of the closure differently, which
#pod can be very useful for debugging.
#pod
#pod =head1 SEE ALSO
#pod
#pod =for :list
#pod * L<Sub::Identify> - for getting information about subs
#pod * L<Sub::Util> - set_subname is another implementation of C<subname>
#pod
#pod =for stopwords cPanel
#pod
#pod =head1 COPYRIGHT AND LICENSE
#pod
#pod This software is copyright (c) 2004, 2008 by Matthijs van Duin, all rights reserved;
#pod copyright (c) 2014 cPanel Inc., all rights reserved.
#pod
#pod This program is free software; you can redistribute it and/or modify
#pod it under the same terms as Perl itself.
#pod
#pod =cut

use 5.006;

use strict;
use warnings;

our $VERSION = '0.14';

use Exporter 5.57 'import';

our @EXPORT = qw(subname);
our @EXPORT_OK = @EXPORT;

use XSLoader;
XSLoader::load(
    __PACKAGE__,
    $VERSION,
);

1;

__END__

=pod

=encoding UTF-8

=head1 NAME

Sub::Name - (re)name a sub

=head1 VERSION

version 0.14

=head1 SYNOPSIS

    use Sub::Name;

    subname $name, $subref;

    $subref = subname foo => sub { ... };

=head1 DESCRIPTION

This module has only one function, which is also exported by default:

=for stopwords subname

=head2 subname NAME, CODEREF

Assigns a new name to referenced sub.  If package specification is omitted in
the name, then the current package is used.  The return value is the sub.

The name is only used for informative routines (caller, Carp, etc).  You won't
be able to actually invoke the sub by the given name.  To allow that, you need
to do glob-assignment yourself.

Note that for anonymous closures (subs that reference lexicals declared outside
the sub itself) you can name each instance of the closure differently, which
can be very useful for debugging.

=head1 SEE ALSO

=over 4

=item *

L<Sub::Identify> - for getting information about subs

=item *

L<Sub::Util> - set_subname is another implementation of C<subname>

=back

=for stopwords cPanel

=head1 AUTHOR

Matthijs van Duin <xmath@cpan.org>

=head1 CONTRIBUTORS

=for stopwords Karen Etheridge Florian Ragwitz Matthijs van Duin Reini Urban Dagfinn Ilmari Mannsåker gfx J.R. Mash

=over 4

=item *

Karen Etheridge <ether@cpan.org>

=item *

Florian Ragwitz <rafl@debian.org>

=item *

Matthijs van Duin <xmath-no-spam@nospam.cpan.org>

=item *

Reini Urban <rurban@cpanel.net>

=item *

Dagfinn Ilmari Mannsåker <ilmari@ilmari.org>

=item *

gfx <gfuji@cpan.org>

=item *

J.R. Mash <jmash.code@gmail.com>

=back

=head1 COPYRIGHT AND LICENSE

This software is copyright (c) 2004, 2008 by Matthijs van Duin, all rights reserved;
copyright (c) 2014 cPanel Inc., all rights reserved.

This program is free software; you can redistribute it and/or modify
it under the same terms as Perl itself.

=cut
