=head1 NAME

PPIx::Regexp::Token::GroupType::Subexpression - Represent an independent subexpression marker

=head1 SYNOPSIS

 use PPIx::Regexp::Dumper;
 PPIx::Regexp::Dumper->new( 'qr{foo(?>bar)}smx' )
     ->print();

=head1 INHERITANCE

C<PPIx::Regexp::Token::GroupType::Subexpression> is a
L<PPIx::Regexp::Token::GroupType|PPIx::Regexp::Token::GroupType>.

C<PPIx::Regexp::Token::GroupType::Subexpression> has no descendants.

=head1 DESCRIPTION

This class represents the '?>' after a left parenthesis that identifies
an independent subexpression.

=head1 METHODS

This class provides no public methods beyond those provided by its
superclass.

=cut

package PPIx::Regexp::Token::GroupType::Subexpression;

use strict;
use warnings;

use base qw{ PPIx::Regexp::Token::GroupType };

our $VERSION = '0.040';

# Return true if the token can be quantified, and false otherwise
# sub can_be_quantified { return };

sub perl_version_introduced {
    my ( $self ) = @_;
    return '5.005';
}

sub __defining_string {
    return '?>';
}

1;

__END__

=head1 SUPPORT

Support is by the author. Please file bug reports at
L<http://rt.cpan.org>, or in electronic mail to the author.

=head1 AUTHOR

Thomas R. Wyant, III F<wyant at cpan dot org>

=head1 COPYRIGHT AND LICENSE

Copyright (C) 2009-2015 by Thomas R. Wyant, III

This program is free software; you can redistribute it and/or modify it
under the same terms as Perl 5.10.0. For more details, see the full text
of the licenses in the directory LICENSES.

This program is distributed in the hope that it will be useful, but
without any warranty; without even the implied warranty of
merchantability or fitness for a particular purpose.

=cut

# ex: set textwidth=72 :
