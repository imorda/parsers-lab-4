package common

import java.lang.RuntimeException

class ParseException(cause: String) : RuntimeException(cause)
