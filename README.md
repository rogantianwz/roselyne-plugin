##为了更好的使用对静态资源进行版本管理，特有如下注意事项以及说明

### 样式文件相关
+ css样式文件中支持相对路径，但是前提是不能在路径外边加引号。
+ 不要在css样式文件中添加类似@charset "uft-8";的声明。

### requirejs文件相关
+ requirejs的配置文件在script标签的data-main属性中定义。
+ requirejs的配置文件中paths模块的key-valve对的valve不能以".js"结尾，也不能以'/'开头，也不能是目录。
+ requirejs所需的模块都必须在paths中声明。

### js相关
+ 如果引用的js库是压缩过的话则引用该库的时候需要加上".min"标识，如：zepto-1.1.3.min.js