=head1 NAME

PPIx::Regexp::Token::GroupType::Code - Represent one of the embedded code indicators

=head1 SYNOPSIS

 use PPIx::Regexp::Dumper;
 PPIx::Regexp::Dumper->new( 'qr{(?{print "hello world!\n")}smx' )
     ->print();

=head1 INHERITANCE

C<PPIx::Regexp::Token::GroupType::Code> is a
L<PPIx::Regexp::Token::GroupType|PPIx::Regexp::Token::GroupType>.

C<PPIx::Regexp::Token::GroupType::Code> has no descendants.

=head1 DESCRIPTION

This method represents one of the embedded code indicators, either '?'
or '??', in the zero-width assertion

 (?{ print "Hello, world!\n" })

or the old-style deferred expression syntax

 my $foo;
 $foo = qr{ foo (??{ $foo }) }smx;

=head1 METHODS

This class provides no public methods beyond those provided by its
superclass.

=cut

package PPIx::Regexp::Token::GroupType::Code;

use strict;
use warnings;

use base qw{ PPIx::Regexp::Token::GroupType };

use PPIx::Regexp::Constant qw{ MINIMUM_PERL };

our $VERSION = '0.040';

# Return true if the token can be quantified, and false otherwise
# sub can_be_quantified { return };

{
    my %perl_version_introduced = (
	'?'	=> '5.005',
	'?p'	=> '5.005',	# Presumed. I can find no documentation.
	'??'	=> '5.006',
    );

    sub perl_version_introduced {
	my ( $self ) = @_;
	return $perl_version_introduced{ $self->unescaped_content() } ||
	    '5.005';
    }

}

{

    my %perl_version_removed = (
	'?p'	=> '5.009005',
    );

    sub perl_version_removed {
	my ( $self ) = @_;
	return $perl_version_removed{ $self->content() };
    }
}

=begin comment

sub __PPIX_TOKENIZER__regexp {
    my ( $class, $tokenizer, $character ) = @_;

    # Recognize '?{', '??{', or '?p{', the latter deprecated in Perl
    # 5.6, and removed in 5.10. The extra escapes are because the
    # non-open-bracket characters may appear as delimiters to the
    # expression.
    if ( my $accept = $tokenizer->find_regexp(
	    qr{ \A \\? \? \\? [?p]? \{ }smx ) ) {

	--$accept;	# Don't want the curly bracket.

	# Code token comes after.
	$tokenizer->expect( 'PPIx::Regexp::Token::Code' );

	return $accept;
    }

    return;
}

=end comment

=cut

sub __defining_string {
    return (
	{ suffix	=> '{' },
	'?',
	'??',
	'?p',
    );
}

sub __match_setup {
    my ( $class, $tokenizer ) = @_;
    $tokenizer->expect( qw{ PPIx::Regexp::Token::Code } );
    return;
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
