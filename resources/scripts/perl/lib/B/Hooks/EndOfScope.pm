package B::Hooks::EndOfScope; # git description: 0.13-15-gab81327
# ABSTRACT: Execute code after a scope finished compilation
# KEYWORDS: code hooks execution scope

use strict;
use warnings;

our $VERSION = '0.14';

# note - a %^H tie() fallback will probably work on 5.6 as well,
# if you need to go that low - sane patches passing *all* tests
# will be gladly accepted
use 5.008001;

BEGIN {
  require Module::Implementation;
  Module::Implementation::build_loader_sub(
    implementations => [ 'XS', 'PP' ],
    symbols => [ 'on_scope_end' ],
  )->();
}

use Sub::Exporter::Progressive -setup => {
  exports => [ 'on_scope_end' ],
  groups  => { default => ['on_scope_end'] },
};

#pod =head1 SYNOPSIS
#pod
#pod     on_scope_end { ... };
#pod
#pod =head1 DESCRIPTION
#pod
#pod This module allows you to execute code when perl finished compiling the
#pod surrounding scope.
#pod
#pod =func on_scope_end
#pod
#pod     on_scope_end { ... };
#pod
#pod     on_scope_end $code;
#pod
#pod Registers C<$code> to be executed after the surrounding scope has been
#pod compiled.
#pod
#pod This is exported by default. See L<Sub::Exporter> on how to customize it.
#pod
#pod =head1 PURE-PERL MODE CAVEAT
#pod
#pod While L<Variable::Magic> has access to some very dark sorcery to make it
#pod possible to throw an exception from within a callback, the pure-perl
#pod implementation does not have access to these hacks. Therefore, what
#pod would have been a compile-time exception is instead converted to a
#pod warning, and your execution will continue as if the exception never
#pod happened.
#pod
#pod To explicitly request an XS (or PP) implementation one has two choices. Either
#pod to import from the desired implementation explicitly:
#pod
#pod  use B::Hooks::EndOfScope::XS
#pod    or
#pod  use B::Hooks::EndOfScope::PP
#pod
#pod or by setting C<$ENV{B_HOOKS_ENDOFSCOPE_IMPLEMENTATION}> to either C<XS> or
#pod C<PP>.
#pod
#pod =head1 SEE ALSO
#pod
#pod L<Sub::Exporter>
#pod
#pod L<Variable::Magic>
#pod
#pod =cut

1;

__END__

=pod

=encoding UTF-8

=head1 NAME

B::Hooks::EndOfScope - Execute code after a scope finished compilation

=head1 VERSION

version 0.14

=head1 SYNOPSIS

    on_scope_end { ... };

=head1 DESCRIPTION

This module allows you to execute code when perl finished compiling the
surrounding scope.

=head1 FUNCTIONS

=head2 on_scope_end

    on_scope_end { ... };

    on_scope_end $code;

Registers C<$code> to be executed after the surrounding scope has been
compiled.

This is exported by default. See L<Sub::Exporter> on how to customize it.

=head1 PURE-PERL MODE CAVEAT

While L<Variable::Magic> has access to some very dark sorcery to make it
possible to throw an exception from within a callback, the pure-perl
implementation does not have access to these hacks. Therefore, what
would have been a compile-time exception is instead converted to a
warning, and your execution will continue as if the exception never
happened.

To explicitly request an XS (or PP) implementation one has two choices. Either
to import from the desired implementation explicitly:

 use B::Hooks::EndOfScope::XS
   or
 use B::Hooks::EndOfScope::PP

or by setting C<$ENV{B_HOOKS_ENDOFSCOPE_IMPLEMENTATION}> to either C<XS> or
C<PP>.

=head1 SEE ALSO

L<Sub::Exporter>

L<Variable::Magic>

=head1 AUTHORS

=over 4

=item *

Florian Ragwitz <rafl@debian.org>

=item *

Peter Rabbitson <ribasushi@cpan.org>

=back

=head1 COPYRIGHT AND LICENSE

This software is copyright (c) 2008 by Florian Ragwitz.

This is free software; you can redistribute it and/or modify it under
the same terms as the Perl 5 programming language system itself.

=head1 CONTRIBUTORS

=for stopwords Karen Etheridge Simon Wilper Tomas Doran

=over 4

=item *

Karen Etheridge <ether@cpan.org>

=item *

Simon Wilper <sxw@chronowerks.de>

=item *

Tomas Doran <bobtfish@bobtfish.net>

=back

=cut
