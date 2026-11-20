package ca.mcscert.jpipe.compiler.steps.transformations;

import ca.mcscert.jpipe.compiler.model.CompilationContext;
import ca.mcscert.jpipe.compiler.model.CompilationException;
import ca.mcscert.jpipe.compiler.model.Transformation;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CoderResult;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;

/**
 * Decodes the source, which must be UTF-8, into the character stream ANTLR
 * lexes.
 *
 * <p>
 * A byte sequence that is not valid UTF-8 is a fatal error, located at the line
 * and column where it sits. It used to be replaced with U+FFFD without a word,
 * so a Latin-1 {@code é} reached every export as {@code �} (#179).
 *
 * <p>
 * A leading byte-order mark, which some Windows editors and PowerShell write at
 * the start of a UTF-8 file, is not part of the source and is skipped.
 */
public final class CharStreamProvider
		extends
			Transformation<InputStream, CharStream> {

	/** The UTF-8 encoding of the byte-order mark, U+FEFF. */
	private static final byte[] BYTE_ORDER_MARK = {(byte) 0xEF, (byte) 0xBB,
			(byte) 0xBF};

	@Override
	protected CharStream run(InputStream input, CompilationContext ctx) {
		byte[] bytes;
		try {
			bytes = input.readAllBytes();
		} catch (IOException e) {
			// Recorded before throwing: `diagnostic` reports from the context,
			// and would otherwise call a source it never read "ok" (#180).
			String message = "cannot read source: " + e.getMessage();
			ctx.fatal(message);
			throw new CompilationException(stepName(), message, e);
		}
		return CharStreams.fromString(decode(bytes, ctx));
	}

	private String decode(byte[] bytes, CompilationContext ctx) {
		CharsetDecoder decoder = StandardCharsets.UTF_8.newDecoder()
				.onMalformedInput(CodingErrorAction.REPORT)
				.onUnmappableCharacter(CodingErrorAction.REPORT);
		int start = startsWithByteOrderMark(bytes) ? BYTE_ORDER_MARK.length : 0;
		ByteBuffer in = ByteBuffer.wrap(bytes, start, bytes.length - start);
		// UTF-8 never decodes to more chars than it has bytes.
		CharBuffer out = CharBuffer.allocate(bytes.length);
		CoderResult result = decoder.decode(in, out, true);
		if (!result.isError()) {
			result = decoder.flush(out);
		}
		out.flip();
		if (result.isError()) {
			throw invalid(out.toString(), bytes[in.position()], ctx);
		}
		return out.toString();
	}

	private static boolean startsWithByteOrderMark(byte[] bytes) {
		return Arrays.equals(bytes, 0,
				Math.min(bytes.length, BYTE_ORDER_MARK.length), BYTE_ORDER_MARK,
				0, BYTE_ORDER_MARK.length);
	}

	/**
	 * Reports the first invalid byte as a FATAL at its location, computed from
	 * the text decoded before it, with the column counted in code points as the
	 * lexer counts them.
	 */
	private CompilationException invalid(String before, byte offending,
			CompilationContext ctx) {
		int lineStart = before.lastIndexOf('\n') + 1;
		int line = (int) before.chars().filter(c -> c == '\n').count() + 1;
		int column = before.codePointCount(lineStart, before.length());
		String message = String.format(
				"invalid UTF-8 byte 0x%02X: the source must be encoded in UTF-8",
				offending & 0xFF);
		ctx.fatal(line, column, message);
		return new CompilationException(stepName(), message);
	}
}
