=head1 NAME

PPIx::Regexp::Element - Base of the PPIx::Regexp hierarchy.

=head1 SYNOPSIS

No user-serviceable parts inside.

=head1 INHERITANCE

C<PPIx::Regexp::Element> is not descended from any other class.

C<PPIx::Regexp::Element> is the parent of
L<PPIx::Regexp::Node|PPIx::Regexp::Node> and
L<PPIx::Regexp::Token|PPIx::Regexp::Token>.

=head1 DESCRIPTION

This class is the base of the L<PPIx::Regexp|PPIx::Regexp>
object hierarchy. It provides the same kind of navigational
functionality that is provided by L<PPI::Element|PPI::Element>.

=head1 METHODS

This class provides the following public methods. Methods not documented
here are private, and unsupported in the sense that the author reserves
the right to change or remove them without notice.

=cut

package PPIx::Regexp::Element;

use strict;
use warnings;

use 5.006;

use Carp;
use List::MoreUtils qw{ firstidx };
use PPIx::Regexp::Util qw{ __instance };
use Scalar::Util qw{ refaddr weaken };

use PPIx::Regexp::Constant qw{ MINIMUM_PERL TOKEN_UNKNOWN };

our $VERSION = '0.040';

=head2 ancestor_of

This method returns true if the object is an ancestor of the argument,
and false otherwise. By the definition of this method, C<$self> is its
own ancestor.

=cut

sub ancestor_of {
    my ( $self, $elem ) = @_;
    __instance( $elem, __PACKAGE__ ) or return;
    my $addr = refaddr( $self );
    while ( $addr != refaddr( $elem ) ) {
	$elem = $elem->_parent() or return;
    }
    return 1;
}

=head2 can_be_quantified

 $token->can_be_quantified()
     and print "This element can be quantified.\n";

This method returns true if the element can be quantified.

=cut

sub can_be_quantified { return 1; }


=head2 class

This method returns the class name of the element. It is the same as
C<ref $self>.

=cut

sub class {
    my ( $self ) = @_;
    return ref $self;
}

=head2 comment

This method returns true if the element is a comment and false
otherwise.

=cut

sub comment {
    return;
}

=head2 content

This method returns the content of the element.

=cut

sub content {
    return;
}

=head2 descendant_of

This method returns true if the object is a descendant of the argument,
and false otherwise. By the definition of this method, C<$self> is its
own descendant.

=cut

sub descendant_of {
    my ( $self, $node ) = @_;
    __instance( $node, __PACKAGE__ ) or return;
    return $node->ancestor_of( $self );
}

=head2 error

 say $token->error();

If an element is one of the classes that represents a parse error, this
method B<may> return a brief message saying why. Otherwise it will
return C<undef>.

=cut

sub error {
    my ( $self ) = @_;
    return $self->{error};
}


=head2 is_quantifier

 $token->is_quantifier()
     and print "This element is a quantifier.\n";

This method returns true if the element is a quantifier. You can not
tell this from the element's class, because a right curly bracket may
represent a quantifier for the purposes of figuring out whether a
greediness token is possible.

=cut

sub is_quantifier { return; }

=head2 modifier_asserted

 $token->modifier_asserted( 'i' )
     and print "Matched without regard to case.\n";

This method returns true if the given modifier is in effect for the
element, and false otherwise.

What it does is to walk backwards from the element until it finds a
modifier object that specifies the modifier, whether asserted or
negated. and returns the specified value. If nobody specifies the
modifier, it returns C<undef>.

This method will not work reliably if called on tokenizer output.

=cut

sub modifier_asserted {
    my ( $self, $modifier ) = @_;

    defined $modifier
	or croak 'Modifier must be defined';

    my $elem = $self;

    while ( $elem ) {
	if ( $elem->can( '__ducktype_modifier_asserted' ) ) {
	    my $val;
	    defined( $val = $elem->__ducktype_modifier_asserted( $modifier ) )
		and return $val;
	}
	if ( my $prev = $elem->sprevious_sibling() ) {
	    $elem = $prev;
	} else {
	    $elem = $elem->parent();
	}
    }

    return;
}

=head2 next_sibling

This method returns the element's next sibling, or nothing if there is
none.

=cut

sub next_sibling {
    my ( $self ) = @_;
    my ( $method, $inx ) = $self->_my_inx()
	or return;
    return $self->_parent()->$method( $inx + 1 );
}

=head2 parent

This method returns the parent of the element, or undef if there is
none.

=cut

sub parent {
    my ( $self ) = @_;
    return $self->_parent();
}

=head2 perl_version_introduced

This method returns the version of Perl in which the element was
introduced. This will be at least 5.000. Before 5.006 I am relying on
the F<perldelta>, F<perlre>, and F<perlop> documentation, since I have
been unable to build earlier Perls. Since I have found no documentation
before 5.003, I assume that anything found in 5.003 is also in 5.000.

Since this all depends on my ability to read and understand masses of
documentation, the results of this method should be viewed with caution,
if not downright skepticism.

There are also cases which are ambiguous in various ways. For those see
L<PPIx::Regexp/RESTRICTIONS>, and especially
L<PPIx::Regexp/Changes in Syntax>.

=cut

sub perl_version_introduced {
    return MINIMUM_PERL;
}

=head2 perl_version_removed

This method returns the version of Perl in which the element was
removed. If the element is still valid the return is C<undef>.

All the I<caveats> to
L<perl_version_introduced()|/perl_version_introduced> apply here also,
though perhaps less severely since although many features have been
introduced since 5.0, few have been removed.

=cut

sub perl_version_removed {
    return undef;	## no critic (ProhibitExplicitReturnUndef)
}

=head2 previous_sibling

This method returns the element's previous sibling, or nothing if there
is none.

=cut

sub previous_sibling {
    my ( $self ) = @_;
    my ( $method, $inx ) = $self->_my_inx()
	or return;
    $inx or return;
    return $self->_parent()->$method( $inx - 1 );
}

=head2 significant

This method returns true if the element is significant and false
otherwise.

=cut

sub significant {
    return 1;
}

=head2 snext_sibling

This method returns the element's next significant sibling, or nothing
if there is none.

=cut

sub snext_sibling {
    my ( $self ) = @_;
    my $sib = $self;
    while ( defined ( $sib = $sib->next_sibling() ) ) {
	$sib->significant() and return $sib;
    }
    return;
}

=head2 sprevious_sibling

This method returns the element's previous significant sibling, or
nothing if there is none.

=cut

sub sprevious_sibling {
    my ( $self ) = @_;
    my $sib = $self;
    while ( defined ( $sib = $sib->previous_sibling() ) ) {
	$sib->significant() and return $sib;
    }
    return;
}

=head2 tokens

This method returns all tokens contained in the element.

=cut

sub tokens {
    my ( $self ) = @_;
    return $self;
}

=head2 top

This method returns the top of the hierarchy.

=cut

sub top {
    my ( $self ) = @_;
    my $kid = $self;
    while ( defined ( my $parent = $kid->_parent() ) ) {
	$kid = $parent;
    }
    return $kid;
}

=head2 unescaped_content

This method returns the content of the element, unescaped.

=cut

sub unescaped_content {
    return;
}

=head2 whitespace

This method returns true if the element is whitespace and false
otherwise.

=cut

sub whitespace {
    return;
}

=head2 nav

This method returns navigation information from the top of the hierarchy
to this node. The return is a list of names of methods and references to
their argument lists. The idea is that given C<$elem> which is somewhere
under C<$top>,

 my @nav = $elem->nav();
 my $obj = $top;
 while ( @nav ) {
     my $method = shift @nav;
     my $args = shift @nav;
     $obj = $obj->$method( @{ $args } ) or die;
 }
 # At this point, $obj should contain the same object
 # as $elem.

=cut

sub nav {
    my ( $self ) = @_;
    __instance( $self, __PACKAGE__ ) or return;

    # We do not use $self->parent() here because PPIx::Regexp overrides
    # this to return the (possibly) PPI object that initiated us.
    my $parent = $self->_parent() or return;

    return ( $parent->nav(), $parent->_nav( $self ) );
}

# Find our location and index among the parent's children. If not found,
# just returns.

{
    my %method_map = (
	children => 'child',
    );
    sub _my_inx {
	my ( $self ) = @_;
	my $parent = $self->_parent() or return;
	my $addr = refaddr( $self );
	foreach my $method ( qw{ children start type finish } ) {
	    $parent->can( $method ) or next;
	    my $inx = firstidx { refaddr $_ == $addr } $parent->$method();
	    $inx < 0 and next;
	    return ( $method_map{$method} || $method, $inx );
	}
	return;
    }
}

{
    my %parent;

    # no-argument form returns the parent; one-argument sets it.
    sub _parent {
	my ( $self, @arg ) = @_;
	my $addr = refaddr( $self );
	if ( @arg ) {
	    my $parent = shift @arg;
	    if ( defined $parent ) {
		__instance( $parent, __PACKAGE__ ) or return;
		weaken(
		    $parent{$addr} = $parent );
	    } else {
		delete $parent{$addr};
	    }
	}
	return $parent{$addr};
    }

    sub _parent_keys {
	return scalar keys %parent;
    }

}

# $self->__impose_defaults( $arg, \%default );
#
# This method can be called in __PPIX_TOKEN__post_make() to supply
# defaults for attributes. It returns nothing.
#
# The arguments are hash references, which are taken in left-to-right
# order, with the, with the first extant value being used.

sub __impose_defaults {
    my ( $self, @args ) = @_;
    foreach my $arg ( @args ) {
	ref $arg eq 'HASH'
	    or next;
	foreach my $key ( keys %{ $arg } ) {
	    exists $self->{$key}
		or $self->{$key} = $arg->{$key};
	}
    }
    return;
}

# Bless into TOKEN_UNKNOWN, record error message, return 1.
sub __error {
    my ( $self, $msg ) = @_;
    $self->isa( 'PPIx::Token::Node' )
	and confess 'Programming error - __error() must be overridden',
	    ' for class ', ref $self;
    defined $msg
	or $msg = 'Was ' . ref $self;
    $self->{error} = $msg;
    bless $self, TOKEN_UNKNOWN;
    return 1;
}

# Called by the lexer to record the capture number.
sub __PPIX_LEXER__record_capture_number {
    my ( $self, $number ) = @_;
    return $number;
}

sub DESTROY {
    $_[0]->_parent( undef );
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
