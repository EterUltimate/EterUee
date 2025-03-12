package me.rerere.rikkahub.ui.pages

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Card
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import me.rerere.rikkahub.ui.components.HighlightCodeBlock
import me.rerere.rikkahub.ui.components.MarkdownBlock
import me.rerere.rikkahub.ui.components.MathInlineText
import me.rerere.rikkahub.ui.context.LocalNavController
import me.rerere.rikkahub.ui.hooks.heroAnimation

val content = """
# 设置
## 这是二级标题
### 这是三级标题
#### 这是四级标题

这里是设置页面的内容

文本**加粗**, *斜体*，~~删除线~~，`代码`

```js
console.log("Hello, World!");
```

${'$'}${'$'}
f(x) = x^2
${'$'}${'$'}

| 标题1 | 标题2 | 标题3 | 
| --- | --- | --- |
| 内容1 | 内容2 | 内容3 |
| 内容1 | 内容2 | 内容3 |

> 这是一个引用

[这是一个链接](https://www.google.com)

- 列表1
- 列表2
- 列表3

1. 列表1
2. 列表2
3. 列表3

- [ ] 任务1
- [x] 任务2
- [ ] 任务3

- [ ] 任务1
  - [x] 子任务1
  - [ ] 子任务2
- [ ] 任务2
- [ ] 任务3

- [ ] 任务1
  - [x]子任务1
  
```js
function helloWorld() {
    console.log("Hello, World!");
}
""".trimIndent()

val formula = """
\f\relax{x} = \int_{-\infty}^\infty
    \f\hat\xi\,e^{2 \pi i \xi x}
    \,d\xi
""".trimIndent()

@Composable
fun ChatPage() {
    val navController = LocalNavController.current
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(text = "设置")
                },
                actions = {

                },
                navigationIcon = {

                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
        ) {
            Card(
                modifier = Modifier.heroAnimation("setting_card"),
                onClick = {
                    navController.navigate("setting")
                }
            ) {
                Box(
                    modifier = Modifier.padding(8.dp)
                ) {
                    Text("设置")
                }
            }


            LazyColumn {
                item {
                    var expr by remember {
                        mutableStateOf("E = mc^2")
                    }
                    MathInlineText(
                        latex = expr,
                        modifier = Modifier
                    )
                    TextField(
                        value = expr,
                        onValueChange = {
                            expr = it
                        },
                        modifier = Modifier
                            .padding(8.dp)
                            .fillMaxWidth()

                    )
                }

                item {
                    HighlightCodeBlock(
                        code = """
                            // say hello
                            function hello() {
                                println("Hello, World!")
                            }

                            const parent = {
                              value: 2,
                              method() {
                                return this.value + 1;
                              },
                            };

                            console.log(parent.method()); // 3
                            // 当调用 parent.method 时，“this”指向了 parent, 当调用 parent.method 时，“this”指向了 parent

                            // child 是一个继承了 parent 的对象
                            const child = {
                              __proto__: parent,
                            };
                            console.log(child.method()); // 3
                            // 调用 child.method 时，“this”指向了 child。
                            // 又因为 child 继承的是 parent 的方法，
                            // 首先在 child 上寻找属性“value”。
                            // 然而，因为 child 没有名为“value”的自有属性，
                            // 该属性会在 [[Prototype]] 上被找到，即 parent.value。

                            child.value = 4; // 将 child 上的属性“value”赋值为 4。
                            // 这会遮蔽 parent 上的“value”属性。
                            // child 对象现在看起来是这样的：
                            // { value: 4, __proto__: { value: 2, method: [Function] } }
                            console.log(child.method()); // 5
                            // 因为 child 现在拥有“value”属性，“this.value”现在表示 child.value

                        """.trimIndent(),
                        language = "js",
                        modifier = Modifier.padding(8.dp)
                    )
                }

                item {
                    MarkdownBlock(content)
                }
            }
        }
    }
}