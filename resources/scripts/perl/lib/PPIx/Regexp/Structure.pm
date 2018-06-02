=head1 NAME

PPIx::Regexp::Structure - Represent a structure.

=head1 SYNOPSIS

 use PPIx::Regexp::Dumper;
 PPIx::Regexp::Dumper->new( 'qr{(foo)}' )->print();

=head1 INHERITANCE

C<PPIx::Regexp::Structure> is a
L<PPIx::Regexp::Node|PPIx::Regexp::Node>.

C<PPIx::Regexp::Structure> is the parent of
L<PPIx::Regexp::Structure::Assertion|PPIx::Regexp::Structure::Assertion>,
L<PPIx::Regexp::Structure::BranchReset|PPIx::Regexp::Structure::BranchReset>,
L<PPIx::Regexp::Structure::Capture|PPIx::Regexp::Structure::Capture>,
L<PPIx::Regexp::Structure::CharClass|PPIx::Regexp::Structure::CharClass>,
L<PPIx::Regexp::Structure::Code|PPIx::Regexp::Structure::Code>,
L<PPIx::Regexp::Structure::Main|PPIx::Regexp::Structure::Main>,
L<PPIx::Regexp::Structure::Modifier|PPIx::Regexp::Structure::Modifier>,
L<PPIx::Regexp::Structure::Quantifier|PPIx::Regexp::Structure::Quantifier>,
L<PPIx::Regexp::Structure::Subexpression|PPIx::Regexp::Structure::Subexpression>,
L<PPIx::Regexp::Structure::Switch|PPIx::Regexp::Structure::Switch> and
L<PPIx::Regexp::Structure::Unknown|PPIx::Regexp::Structure::Unknown>.

=head1 DESCRIPTION

This class represents a bracketed construction of some sort. The
brackets considered part of the structure, but not inside it. So the
C<elements()> method returns the brackets if they are defined, but the
C<children()> method does not.

=head1 METHODS

This class provides the following public methods. Methods not documented
here are private, and unsupported in the sense that the author reserves
the right to change or remove them without notice.

=cut

package PPIx::Regexp::Structure;

use strict;
use warnings;

use base qw{ PPIx::Regexp::Node };

use Carp qw{ confess };
use PPIx::Regexp::Constant qw{ STRUCTURE_UNKNOWN };
use PPIx::Regexp::Util qw{ __instance };
use Scalar::Util qw{ refaddr };

our $VERSION = '0.040';

sub _new {
    my ( $class, @args ) = @_;
    my %brkt;
    if ( ref $args[0] eq 'HASH' ) {
	%brkt = %{ shift @args };
	foreach my $key ( qw{ start type finish } ) {
	    ref $brkt{$key} eq 'ARRAY' or $brkt{$key} = [ $brkt{$key} ];
	}
    } else {
	$brkt{finish} = [ @args ? pop @args : () ];
	$brkt{start} = [ @args ? shift @args : () ];
	while ( @args && ! $args[0]->significant() ) {
	    push @{ $brkt{start} }, shift @args;
	}
	$brkt{type} = [];
	if ( __instance( $args[0], 'PPIx::Regexp::Token::GroupType' ) ) {
	    push @{ $brkt{type} }, shift @args;
	    while ( @args && ! $args[0]->significant() ) {
		push @{ $brkt{type} }, shift @args;
	    }
	}
    }

    $class->_check_for_interpolated_match( \%brkt, \@args );

    my $self = $class->SUPER::_new( @args )
	or return;

    if ( __instance( $brkt{type}[0], 'PPIx::Regexp::Token::GroupType' ) ) {
	( my $reclass = ref $brkt{type}[0] ) =~
	    s/ Token::GroupType /Structure/smx;
	$reclass->can( 'start' )
	    or confess "Programming error - $reclass not loaded";
	bless $self, $reclass;
    }

    foreach my $key ( qw{ start type finish } ) {
	$self->{$key} = [];
	ref $brkt{$key} eq 'ARRAY'
	    or confess "Programming error - '$brkt{$key}' not an ARRAY";
	foreach my $val ( @{ $brkt{$key} } ) {
	    defined $val or next;
	    __instance( $val, 'PPIx::Regexp::Element' )
		or confess "Programming error - '$val' not a ",
		    "PPIx::Regexp::Element";
	    push @{ $self->{$key} }, $val;
	    $val->_parent( $self );
	}
    }
    return $self;
}

sub elements {
    my ( $self ) = @_;

    if ( wantarray ) {
	return (
	    @{ $self->{start} },
	    @{ $self->{type} },
	    @{ $self->{children} },
	    @{ $self->{finish} },
	);
    } elsif ( defined wantarray ) {
	my $size = scalar @{ $self->{start} };
	$size += scalar @{ $self->{type} };
	$size += scalar @{ $self->{children} };
	$size += scalar @{ $self->{finish} };
	return $size;
    } else {
	return;
    }
}

=head2 finish

 my $elem = $struct->finish();
 my @elem = $struct->finish();
 my $elem = $struct->finish( 0 );

Returns the finishing structure element. This is included in the
C<elements> but not in the C<children>.

The finishing element is actually an array, though it should never have
more than one element. Calling C<finish> in list context gets you all
elements of the array. Calling it in scalar context gets you an element
of the array, defaulting to element 0 if no argument is passed.

=cut

sub finish {
    my ( $self, $inx ) = @_;
    wantarray and return @{ $self->{finish} };
    return $self->{finish}[ defined $inx ? $inx : 0 ];
}

sub first_element {
    my ( $self ) = @_;

    $self->{start}[0] and return $self->{start}[0];

    $self->{type}[0] and return $self->{type}[0];

    if ( my $elem = $self->SUPER::first_element() ) {
	return $elem;
    }

    $self->{finish}[0] and return $self->{finish}[0];

    return;
}

sub last_element {
    my ( $self ) = @_;

    $self->{finish}[-1] and return $self->{finish}[-1];

    if ( my $elem = $self->SUPER::last_element() ) {
	return $elem;
    }

    $self->{type}[-1] and return $self->{type}[-1];

    $self->{start}[-1] and return $self->{start}[-1];

    return;
}

=head2 start

 my $elem = $struct->start();
 my @elem = $struct->start();
 my $elem = $struct->start( 0 );

Returns the starting structure element. This is included in the
C<elements> but not in the C<children>.

The starting element is actually an array. The first element (element 0)
is the actual starting delimiter. Subsequent elements, if any, are
insignificant elements (comments or white space) absorbed into the start
element for ease of parsing subsequent elements.

Calling C<start> in list context gets you all elements of the array.
Calling it in scalar context gets you an element of the array,
defaulting to element 0 if no argument is passed.

=cut

sub start {
    my ( $self, $inx ) = @_;
    wantarray and return @{ $self->{start} };
    return $self->{start}[ defined $inx ? $inx : 0 ];
}

=head2 type

 my $elem = $struct->type();
 my @elem = $struct->type();
 my $elem = $struct->type( 0 );

Returns the group type if any. This will be the leading
L<PPIx::Regexp::Token::GroupType|PPIx::Regexp::Token::GroupType>
token if any. This is included in C<elements> but not in C<children>.

The type is actually an array. The first element (element 0) is the
actual type determiner. Subsequent elements, if any, are insignificant
elements (comments or white space) absorbed into the type element for
consistency with the way the start element is handled.

Calling C<type> in list context gets you all elements of the array.
Calling it in scalar context gets you an element of the array,
defaulting to element 0 if no argument is passed.

=cut

sub type {
    my ( $self, $inx ) = @_;
    wantarray and return @{ $self->{type} };
    return $self->{type}[ defined $inx ? $inx : 0 ];
}

# Check for things like (?$foo:...) or (?$foo)
sub _check_for_interpolated_match {
    my ( $class, $brkt, $args ) = @_;

    # Everything we are interested in begins with a literal '?' followed
    # by an interpolation.
    __instance( $args->[0], 'PPIx::Regexp::Token::Unknown' )
	and $args->[0]->content() eq '?'
	and __instance( $args->[1], 'PPIx::Regexp::Token::Interpolation' )
	or return;

    my $hiwater = 2;	# Record how far we got into the arguments for
    			# subsequent use detecting things like
			# (?$foo).

    # If we have a literal ':' as the third argument:
    # GroupType::Modifier, rebless the ':' so we know not to match
    # against it, and splice all three tokens into the type.
    if ( __instance( $args->[2], 'PPIx::Regexp::Token::Literal' )
	&& $args->[2]->content() eq ':' ) {

	# Rebless the '?' as a GroupType::Modifier.
	bless $args->[0], 'PPIx::Regexp::Token::GroupType::Modifier';
	# Note that we do _not_ want __PPIX_TOKEN__post_make here.

	# Rebless the ':' as a GroupType, just so it does not look like
	# something to match against.
	bless $args->[2], 'PPIx::Regexp::Token::GroupType';

	# Shove our three significant tokens into the type.
	push @{ $brkt->{type} }, splice @{ $args }, 0, 3;

	# Stuff all the immediately-following insignificant tokens into
	# the type as well.
	while ( @{ $args } && ! $args->[0]->significant() ) {
	    push @{ $brkt->{type} }, shift @{ $args };
	}

	# Return to the caller, since we have done all the damage we
	# can.
	return;
    }

    # If we have a literal '-' as the third argument, we might have
    # something like (?$on-$off:$foo).
    if ( __instance( $args->[2], 'PPIx::Regexp::Token::Literal' )
	&& $args->[2]->content() eq '-'
	&& __instance( $args->[3], 'PPIx::Regexp::Token::Interpolation' )
    ) {
	$hiwater = 4;

	if ( __instance( $args->[4], 'PPIx::Regexp::Token::Literal' )
	    && $args->[4]->content() eq ':' ) {

	    # Rebless the '?' as a GroupType::Modifier.
	    bless $args->[0], 'PPIx::Regexp::Token::GroupType::Modifier';
	    # Note that we do _not_ want __PPIX_TOKEN__post_make here.

	    # Rebless the '-' and ':' as GroupType, just so they do not
	    # look like something to match against.
	    bless $args->[2], 'PPIx::Regexp::Token::GroupType';
	    bless $args->[4], 'PPIx::Regexp::Token::GroupType';

	    # Shove our five significant tokens into the type.
	    push @{ $brkt->{type} }, splice @{ $args }, 0, 5;

	    # Stuff all the immediately-following insignificant tokens
	    # into the type as well.
	    while ( @{ $args } && ! $args->[0]->significant() ) {
		push @{ $brkt->{type} }, shift @{ $args };
	    }

	    # Return to the caller, since we have done all the damage we
	    # can.
	    return;
	}
    }

    # If the group contains _any_ significant tokens at this point, we
    # do _not_ have something like (?$foo).
    foreach my $inx ( $hiwater .. $#$args ) {
	$args->[$inx]->significant() and return;
    }

    # Rebless the '?' as a GroupType::Modifier.
    bless $args->[0], 'PPIx::Regexp::Token::GroupType::Modifier';
    # Note that we do _not_ want __PPIX_TOKEN__post_make here.

    # Shove all the contents of $args into type, using splice to leave
    # @{ $args } empty after we do this.
    push @{ $brkt->{type} }, splice @{ $args };

    # We have done all the damage we can.
    return;
}

sub __error {
    my ( $self, $msg ) = @_;
    defined $msg
	or $msg = 'Was class ' . ref $self;
    $self->{error} = $msg;
    bless $self, STRUCTURE_UNKNOWN;
    return 1;
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
