package Devel::OverloadInfo;
$Devel::OverloadInfo::VERSION = '0.002';
# ABSTRACT: introspect overloaded operators

# =head1 DESCRIPTION
#
# Devel::OverloadInfo returns information about L<overloaded|overload>
# operators for a given class (or object), including where in the
# inheritance hierarchy the overloads are declared and where the code
# implementing it is.
#
# =cut

use strict;
use warnings;
use overload ();
use Scalar::Util qw(blessed);
use Sub::Identify qw(sub_fullname);
use Package::Stash 0.14;
use MRO::Compat;

use Exporter 5.57 qw(import);
our @EXPORT_OK = qw(overload_info);

sub stash_with_symbol {
    my ($class, $symbol) = @_;

    for my $package (@{mro::get_linear_isa($class)}) {
        my $stash = Package::Stash->new($package);
        my $value_ref = $stash->get_symbol($symbol);
        return ($stash, $value_ref) if $value_ref;
    }
    return;
}

# =func overload_info
#
#     my $info = overload_info($class_or_object);
#
# Returns a hash reference with information about all the overloaded
# operators of the argument, which can be either a class name or a blessed
# object. The keys are the overloaded operators, as specified in
# C<%overload::ops> (see L<overload/Overloadable Operations>).
#
# =over
#
# =item class
#
# The name of the class in which the operator overloading was declared.
#
# =item code
#
# A reference to the function implementing the overloaded operator.
#
# =item code_name
#
# The name of the function implementing the overloaded operator, as
# returned by C<sub_fullname> in L<Sub::Identify>.
#
# =item method_name (optional)
#
# The name of the method implementing the overloaded operator, if the
# overloading was specified as a named method, e.g. C<< use overload $op
# => 'method'; >>.
#
# =item code_class (optional)
#
# The name of the class in which the method specified by C<method_name>
# was found.
#
# =item value (optional)
#
# For the special C<fallback> key, the value it was given in C<class>.
#
# =back
#
# =cut

sub overload_info {
    my $class = blessed($_[0]) || $_[0];

    return undef unless overload::Overloaded($class);

    my (%overloaded);
    for my $op (map split(/\s+/), values %overload::ops) {
        my $op_method = $op eq 'fallback' ? "()" : "($op";
        my ($stash, $func) = stash_with_symbol($class, "&$op_method")
            or next;
        my $info = $overloaded{$op} = {
            class => $stash->name,
        };
        if ($func == \&overload::nil) {
            # Named method or fallback, stored in the scalar slot
            if (my $value_ref = $stash->get_symbol("\$$op_method")) {
                my $value = $$value_ref;
                if ($op eq 'fallback') {
                    $info->{value} = $value;
                } else {
                    $info->{method_name} = $value;
                    if (my ($impl_stash, $impl_func) = stash_with_symbol($class, "&$value")) {
                        $info->{code_class} = $impl_stash->name;
                        $info->{code} = $impl_func;
                    }
                }
            }
        } else {
            $info->{code} = $func;
        }
        $info->{code_name} = sub_fullname($info->{code})
            if exists $info->{code};
    }
    return \%overloaded;
}

1;

__END__

=pod

=encoding UTF-8

=head1 NAME

Devel::OverloadInfo - introspect overloaded operators

=head1 VERSION

version 0.002

=head1 DESCRIPTION

Devel::OverloadInfo returns information about L<overloaded|overload>
operators for a given class (or object), including where in the
inheritance hierarchy the overloads are declared and where the code
implementing it is.

=head1 FUNCTIONS

=head2 overload_info

    my $info = overload_info($class_or_object);

Returns a hash reference with information about all the overloaded
operators of the argument, which can be either a class name or a blessed
object. The keys are the overloaded operators, as specified in
C<%overload::ops> (see L<overload/Overloadable Operations>).

=over

=item class

The name of the class in which the operator overloading was declared.

=item code

A reference to the function implementing the overloaded operator.

=item code_name

The name of the function implementing the overloaded operator, as
returned by C<sub_fullname> in L<Sub::Identify>.

=item method_name (optional)

The name of the method implementing the overloaded operator, if the
overloading was specified as a named method, e.g. C<< use overload $op
=> 'method'; >>.

=item code_class (optional)

The name of the class in which the method specified by C<method_name>
was found.

=item value (optional)

For the special C<fallback> key, the value it was given in C<class>.

=back

=head1 AUTHOR

Dagfinn Ilmari Mannsåker <ilmari@ilmari.org>

=head1 COPYRIGHT AND LICENSE

This software is copyright (c) 2014 by Dagfinn Ilmari Mannsåker.

This is free software; you can redistribute it and/or modify it under
the same terms as the Perl 5 programming language system itself.

=cut
