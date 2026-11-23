require "formula"

# Template of the jpipe formula in jpipe-mcscert/homebrew-mcscert. The
# update-homebrew job of release.yml fills in @@URL@@ and @@SHA256@@ and writes
# the whole file to the tap, as jpipe.rb and as jpipe@X.Y.Z.rb: the tap is
# never edited by hand (ADR-0025).
class Jpipe < Formula

    homepage "https://github.com/jpipe-mcscert/jpipe-compiler"

    url "@@URL@@"
    sha256 "@@SHA256@@"

    depends_on "graphviz"
    depends_on "openjdk@25"


    def install
        inreplace "jpipe", "@@PREFIX@@", "#{prefix}"
        inreplace "jpipe", "@@JAVA@@", Formula["openjdk@25"].opt_bin/"java"
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
end
