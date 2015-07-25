package utils

import views.html.bootstrap.bootstrapInlineFieldConstructorTemplate

/**
 * @author joao
 */
object MyHelpers {
  object BootstrapInline {
    import views.html.helper.FieldConstructor
    implicit val inlineFields = FieldConstructor(bootstrapInlineFieldConstructorTemplate.f)
  }
}