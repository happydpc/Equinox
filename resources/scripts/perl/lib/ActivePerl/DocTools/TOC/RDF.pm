package ActivePerl::DocTools::TOC::RDF;

use strict;
use warnings;

use base 'ActivePerl::DocTools::TOC';

my $_section    = 1;
my $_subsection = 0;

sub text {
    my $text =  join("\n", @_, "");
    return sub { $text };
}

#my @begin_subhead = ("<nc:subheadings>","<rdf:Seq>");
my @begin_subhead = (" ");

#*end_subhead = text("</rdf:Seq>","</nc:subheadings>");
#*end_subhead = text("    </rdf:Seq>", "  </nc:subheadings>", "</rdf:Description>");
*end_subhead = text (" ");

*boilerplate = text(<<HERE);
<?xml version="1.0"?>

<rdf:RDF xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#"
         xmlns:nc="http://home.netscape.com/NC-rdf#"
         xmlns:kc="http://www.activestate.com/KC-rdf#">

  <rdf:Description about="urn:root">
    <nc:subheadings>
      <rdf:Seq>
        <rdf:li>
          <rdf:Description ID="ActivePerl-doc-top" nc:name="ActivePerl Documentation" nc:link="ActivePerl/perlmain.html">
            <nc:subheadings>
              <rdf:Seq>
                <rdf:li>
                  <rdf:Description ID="ActivePerl-GS" nc:name="Getting Started"/>
                  <rdf:Description ID="ActivePerl-IN" nc:name="Install Notes"/>
                  <rdf:Description ID="ActivePerl-AC" nc:name="ActivePerl Components"/>
                  <rdf:Description ID="ActivePerl-AF" nc:name="ActivePerl FAQ"/>
                  <rdf:Description ID="ActivePerl-WS" nc:name="Windows Scripting"/>
                  <rdf:Description ID="ActivePerl-1" nc:name="Core Perl"/>
                  <rdf:Description ID="ActivePerl-2" nc:name="Programs"/>
                  <rdf:Description ID="ActivePerl-3" nc:name="Pragmas"/>
                  <rdf:Description ID="ActivePerl-4" nc:name="Libraries"/>
                </rdf:li>
              </rdf:Seq>
            </nc:subheadings>
        </rdf:Description>
        </rdf:li>
      </rdf:Seq>
    </nc:subheadings>
  </rdf:Description>

  <rdf:Description about="#ActivePerl-GS" nc:name="Getting Started">
    <nc:subheadings>
      <rdf:Seq>
        <rdf:li>
          <rdf:Description nc:name="Welcome To ActivePerl" nc:link="ActivePerl/perlmain.html"/>
          <rdf:Description nc:name="Release Notes" nc:link="ActivePerl/release.html"/>
          <rdf:Description nc:name="Readme" nc:link="ActivePerl/readme.html"/>
          <rdf:Description nc:name="ActivePerl Change Log" nc:link="ActivePerl/changes.html"/>
        </rdf:li>
      </rdf:Seq>
    </nc:subheadings>
  </rdf:Description>

  <rdf:Description about="#ActivePerl-IN" nc:name="Install Notes">
    <nc:subheadings>
      <rdf:Seq>
        <rdf:li>
          <rdf:Description nc:name="Linux" nc:link="ActivePerl/faq/Linux/Install.html"/>
          <rdf:Description nc:name="Solaris" nc:link="ActivePerl/faq/Solaris/Install.html"/>
          <rdf:Description nc:name="Windows" nc:link="ActivePerl/faq/Windows/Install.html"/>
        </rdf:li>
      </rdf:Seq>
    </nc:subheadings>
  </rdf:Description>

  <rdf:Description about="#ActivePerl-AF" nc:name="ActivePerl FAQ">
    <nc:subheadings>
      <rdf:Seq>
        <rdf:li>
          <rdf:Description nc:name="Introduction" nc:link="ActivePerl/faq/ActivePerl-faq.html"/>
          <rdf:Description nc:name="Availability &amp; Install" nc:link="ActivePerl/faq/ActivePerl-faq1.html"/>
          <rdf:Description nc:name="Availability &amp; Install" nc:link="ActivePerl/faq/ActivePerl-faq1.html"/>
          <rdf:Description nc:name="Using PPM" nc:link="ActivePerl/faq/ActivePerl-faq2.html"/>
          <rdf:Description nc:name="Docs &amp; Support" nc:link="ActivePerl/faq/ActivePerl-faq3.html"/>
        </rdf:li>
      </rdf:Seq>
    </nc:subheadings>
  </rdf:Description>

HERE

*header = text(<<HERE);

HERE


# *before_pods = text("<!-- Core Perl Documentation -->",@begin_subhead);
sub before_pods {
    my($self, $file) = @_;

    return
    "  <rdf:Description about=\"#ActivePerl-$_section\">\n".
    "    <nc:subheadings>\n".
    "      <rdf:Seq>\n";
}


#*pod_separator = text(" <rdf:li>"," </rdf:li>");
sub pod_separator {
    $_subsection++;
    return
    "        <rdf:li>\n".
    "          <rdf:Description ID=\"ActivePerl-$_section-$_subsection\"\n".
    "                           nc:name=\" \"/>\n".
    "        </rdf:li>\n";
}

sub pod {
    my($self, $file) = @_;
    my $key = $^O eq "darwin" ? "pods::$file" : "Pod::$file";
    return
    "        <rdf:li>\n".
    rdf_li_desc($file, 'Perl/' . $self->{'pods'}->{$key}).
    "          </rdf:Description>\n".
    "        </rdf:li>\n";
}

sub rdf_li_desc {
    my($name, $link) = @_;
    $_subsection++;
    return
    "          <rdf:Description ID=\"ActivePerl-$_section-$_subsection\"\n".
    "                           nc:name=\"$name\"\n".
    "                           nc:link=\"$link\">\n";
}

#*after_pods = \&end_subhead;
sub after_pods {
    $_section++;
    return
    "    </rdf:Seq>\n".
    "  </nc:subheadings>\n".
    "</rdf:Description>\n";
}

#*before_scripts = text("<!-- Programs -->",@begin_subhead);
sub before_scripts {
    return
    "  <rdf:Description about=\"#ActivePerl-$_section\">\n".
    "    <nc:subheadings>\n".
    "      <rdf:Seq>\n";
}

sub script {
    my($self, $file) = @_;
    return
    "        <rdf:li>\n".
    rdf_li_desc($file, 'Perl/' . $self->{'pragmaz'}->{$file}).
    "          </rdf:Description>\n".
    "        </rdf:li>\n";
}

#*after_scripts = \&end_subhead;
sub after_scripts {
    $_section++;
    return
    "    </rdf:Seq>\n".
    "  </nc:subheadings>\n".
    "</rdf:Description>\n";
}

#*before_pragmas = text("<!-- Pragmas -->",@begin_subhead);
sub before_pragmas {
    return
    "  <rdf:Description about=\"#ActivePerl-$_section\">\n".
    "    <nc:subheadings>\n".
    "      <rdf:Seq>\n";
}

sub pragma {
    my($self, $file) = @_;
    return
    "        <rdf:li>\n".
    rdf_li_desc($file, 'Perl/' . $self->{'pragmaz'}->{$file}).
    "          </rdf:Description>\n".
    "        </rdf:li>\n";
}

#*after_pragmas = \&end_subhead;
sub after_pragmas {
    $_section++;
    return 
    "    </rdf:Seq>\n".
    "  </nc:subheadings>\n".
    "</rdf:Description>\n";
}

#*before_libraries = text("<!-- Libraries -->",@begin_subhead);
sub before_libraries {
    return
    "  <rdf:Description about=\"#ActivePerl-$_section\">\n".
    "    <nc:subheadings>\n".
    "      <rdf:Seq>\n";
}


#*library_indent_open = text(@begin_subhead);
#*library_indent_close = \&end_subhead;

sub library_indent_open {
    return
    "    <nc:subheadings>\n".
    "      <rdf:Seq>\n";
}

sub library_indent_close {
    return
    "          </rdf:Description>\n".
    "        </rdf:li>\n".
    "      </rdf:Seq>\n".
    "    </nc:subheadings>\n".
    "          </rdf:Description>\n".
    "        </rdf:li>\n";
}

sub library_indent_same {
    return
    "          </rdf:Description>\n".
    "        </rdf:li>\n";
}

sub library {
    my($self, $file, $showfile) = @_;
    return
    "        <rdf:li>\n".
    rdf_li_desc($showfile, 'Perl/' . $self->{'filez'}->{$file});
}

sub library_container {
    my($self, $file, $showfile) = @_;
    return
    " <rdf:li>\n".
    "  <rdf:Description nc:name=\"$showfile\">\n";
}

#*after_libraries = \&end_subhead;
sub after_libraries {
    $_section++;
    return 
    "    </rdf:Seq>\n".
    "  </nc:subheadings>\n".
    "</rdf:Description>\n";
}

*footer = text("</rdf:RDF>");

1;
