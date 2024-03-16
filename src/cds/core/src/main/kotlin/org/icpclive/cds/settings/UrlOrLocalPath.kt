package org.icpclive.cds.settings

import java.nio.file.Path

public sealed class UrlOrLocalPath {
    public abstract fun subDir(s: String): UrlOrLocalPath
    public class Url(public val value: String) : UrlOrLocalPath() {
        public override fun subDir(s: String): UrlOrLocalPath = Url("${value.removeSuffix("/")}/$s")
        override fun toString(): String = value
    }

    public class Local(public val value: Path) : UrlOrLocalPath() {
        public override fun subDir(s: String): UrlOrLocalPath = Local(value.resolve(s))
        override fun toString(): String = value.toString()
    }
}