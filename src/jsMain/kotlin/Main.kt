import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import org.jetbrains.compose.web.css.CSSMediaQuery
import org.jetbrains.compose.web.css.Color
import org.jetbrains.compose.web.css.DisplayStyle
import org.jetbrains.compose.web.css.FlexDirection
import org.jetbrains.compose.web.css.FlexWrap
import org.jetbrains.compose.web.css.JustifyContent
import org.jetbrains.compose.web.css.Style
import org.jetbrains.compose.web.css.StyleSheet
import org.jetbrains.compose.web.css.borderRadius
import org.jetbrains.compose.web.css.color
import org.jetbrains.compose.web.css.display
import org.jetbrains.compose.web.css.flexDirection
import org.jetbrains.compose.web.css.flexWrap
import org.jetbrains.compose.web.css.fontFamily
import org.jetbrains.compose.web.css.justifyContent
import org.jetbrains.compose.web.css.margin
import org.jetbrains.compose.web.css.media
import org.jetbrains.compose.web.css.mediaMaxWidth
import org.jetbrains.compose.web.css.minHeight
import org.jetbrains.compose.web.css.percent
import org.jetbrains.compose.web.css.px
import org.jetbrains.compose.web.css.textDecoration
import org.jetbrains.compose.web.css.vh
import org.jetbrains.compose.web.css.width
import org.jetbrains.compose.web.dom.A
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.Img
import org.jetbrains.compose.web.dom.Span
import org.jetbrains.compose.web.dom.Text
import org.jetbrains.compose.web.renderComposable

fun main() {
    renderComposable(rootElementId = "root") {
        Div({
            style {
                display(DisplayStyle.Flex)
                flexDirection(FlexDirection.Column)
                justifyContent(JustifyContent.Center)
                minHeight(100.vh)
            }
        }) {
            ProfilePhoto()
            Name()
            Links()
        }
    }
}

@Composable
fun ProfilePhoto() {
    Img(
        src = "https://avatars3.githubusercontent.com/u/8710980?s=460&u=da1b03b6cb20cef510f66180f0b7501c19b78d55&v=4",
        attrs = {
            style {
                display(DisplayStyle.Block)
                property("margin", "0 auto")
                width(150.px)
                borderRadius(50.percent)
            }
        }
    )
}

@Composable
fun Name() {
    Span(
        attrs = {
            style {
                property("margin", "15px auto")
                property("font-family","Roboto, sans-serif")
                color(Color.white)
            }
        }
    ) { Text("Alex Gabor") }
}

object LinkStyle : StyleSheet() {
        val container by style {
        display(DisplayStyle.Flex)
        flexDirection(FlexDirection.Row)
        justifyContent(JustifyContent.Center)
        flexWrap(FlexWrap.Wrap)

        media(CSSMediaQuery.And(mutableListOf(CSSMediaQuery.MediaType(CSSMediaQuery.MediaType.Enum.Screen), mediaMaxWidth(420.px)))) {
            self style {
                flexDirection(FlexDirection.Column)
            }
        }
    }
    val linkItem by style {
        property("font-family","Roboto, sans-serif")
        margin(20.px)
        textDecoration("none")
        color(Color("#ee6c4d"))

        self + visited + hover + focus style {
            textDecoration("none")
            color(Color("#ee6c4d"))
        }
        media(mediaMaxWidth(420.px)) {
            self style {
                property("margin", "20px auto")
            }
        }
    }
}

@Composable
fun Links() {
    Style(LinkStyle)
    Div(
        attrs = {
            classes(LinkStyle.container)
        }
    ) {
        Link("https://github.com/AlexGabor", "Github")
        Link("https://twitter.com/AlexGabor42", "Twitter")
        Link("https://www.instagram.com/alexandru_gabor/", "Instagram")
    }
}

@Composable
fun Link(
    href: String,
    text: String,
) {
    A(href, attrs = {
        classes(LinkStyle.linkItem)
    }) {
        Text(text)
    }
}
