# typed: strict
# frozen_string_literal: true

# Template of the jpipe formula in jpipe-mcscert/homebrew-mcscert. The
# update-homebrew job of release.yml fills in the url and sha256 placeholders
# and writes the whole file to the tap, as jpipe.rb and as jpipe@X.Y.Z.rb: the
# tap is never edited by hand (ADR-0025).
class Jpipe < Formula
  desc "Compiler for justification models (Justified Pipelines)"
  homepage "https://github.com/jpipe-mcscert/jpipe-compiler"
  url "@@URL@@"
  sha256 "@@SHA256@@"

  depends_on "graphviz"
  depends_on "openjdk@25"

  def install
    inreplace "jpipe", "@@PREFIX@@", prefix.to_s
    inreplace "jpipe", "@@JAVA@@", formula_opt_bin("openjdk@25")/"java"
    prefix.install "jpipe.jar"
    prefix.install "aot-training.jd"
    bin.install "jpipe"
  end

  # An AOT cache makes every call start faster, and only works on the
  # machine that created it (#216). jPipe runs without it, so a failure here
  # does not fail the install. After upgrading openjdk@25, run
  # `brew postinstall jpipe` to create it again.
  post_install_steps do
    run "jpipe", base: :bin, args: ["--create-aot-cache"],
                 must_succeed: false, print_stderr: false
  end

  test do
    assert_match "jPipe #{version}", shell_output("#{bin}/jpipe --version")
  end
end
