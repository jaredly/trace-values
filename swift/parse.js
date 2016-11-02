
const rx = {
  ident: /^[a-z_]+/,
  identEq: /^[a-zA-Z_]+=/,
  white: /^ */,
  whiteNL: /^[ \r\n\s]*/,
}

const nameRx = '[a-zA-Z_][a-zA-Z_0-9]*'
const locRx = '(/[^/:\\[\\]]+)+:\\d+:\\d+'
const pathItem = '(' + nameRx + '|\\(file\\)|%|/|>)'
const fullPath = '(' + pathItem + '\\.)+' + pathItem

const vrx = {
  dstr: /^"([^"]|\\[^\n])*"/,
  sstr: /^'([^']|\\[^\n])*'/,
  decl: new RegExp('^' + fullPath + '\\(([a-zA-Z_]+:)*\\)'),
  numbers: /^\[(-?\d+,\s*)*(-?\d+)?\]/,
  fullPath: new RegExp('^' + fullPath),
  location: new RegExp('^' + locRx),
  range: new RegExp('^\\[' + locRx + ' - line:\\d+:\\d+\\]'),
  ident: /^[a-z_A-Z@0-9:]+/,
}

const getNestedList = (text, i) => {
  let num = 0
  for (let z = i; z < text.length; z++) {
    if (text[z] === '[') {
      num += 1
    } else if (text[z] === ']') {
      num -= 1
      if (num === 0) {
        return {i: z + 1, attr: {type: 'nested_list', text: text.slice(i, z + 1)}}
      }
    }
  }
  return null
}

const getVal = (text, i) => {
  for (var name in vrx) {
    const match = prx(text, i, vrx[name])
    if (match) {
      return {i: i + match.length, attr: {type: name, text: match}}
    }
  }
  if (text[i] == '[') {
    return getNestedList(text, i)
  }
  return null
}

const getValOrArr = (text, i) => {
  const val = getVal(text, i)
  if (!val || text[val.i] !== ',') return val
  const items = [val.attr]
  i = val.i
  while (text[i] === ',') {
    const val = getVal(text, i + 1)
    if (!val) return {i, attr: {type: 'array', items}}
    items.push(val.attr)
    i = val.i
  }
  return {i, attr: {type: 'array', items}}
}

const getEq = (text, i) => {
  const name = prx(text, i, rx.identEq)
  if (!name) return false
  const res = getValOrArr(text, i + name.length)
  if (!res) return false
  return {i: res.i, attr: {name: name.slice(0, -1), val: res.attr}}
}

const getOneAttr = (text, i) => {
  let res = getEq(text, i)
  if (res) return res
  return getVal(text, i)
}

const parseAttrs = text => {
  // if (1) return text
  let i = 0
  const attrs = []
  while (i < text.length) {
    const res = getOneAttr(text, i)
    if (!res) {
      // console.warn('Attr parse', i, JSON.stringify([text.slice(i, i + 10), text.slice(i - 10, i + 10)]))
      // throw new Error('Unable to parse all attrs: ' + text.slice(i))
      attrs.push({type: 'rest', text: text.slice(i)})
      break
    }
    attrs.push(res.attr)
    i = skipWhite(text, res.i)
  }
  // attrs.push(text) // TODO remove, just for debug
  return attrs
}

const skipWhite = (text, i) => {
  return text.slice(i).match(rx.white)[0].length + i
}

const skipWhiteNL = (text, i) => {
  return text.slice(i).match(rx.whiteNL)[0].length + i
}

const prx = (text, i, rx) => {
  const match = text.slice(i).match(rx)
  return match ? match[0] : null
}

const parseChildren = (text, i) => {
  // console.log('a', text[i], i)
  i = skipWhite(text, i)
  // console.log('a', text[i], i)
  const children = []
  while (text[i] === '(') {
    let res = parseExpr(text, i)
    children.push(res.expr)
    i = skipWhiteNL(text, res.i)
  }
  return {children, i}
}

const parseExpr = (text, i) => {
  i = skipWhite(text, i)
  if (text[i] !== '(') {
    console.log('ERR no open paren', text[i])
    return {error: 'no open paren'}
  }
  i += 1
  const name = prx(text, i, rx.ident)
  if (!name) {
    console.log('ERR no name', text[i])
    return {error: 'no name'}
  }
  i = skipWhite(text, i + name.length)
  const nl = text.indexOf('\n', i)
  const attrsRaw = text.slice(i, nl)
  const trailingParens = attrsRaw.match(/\)*$/)[0].length
  const done = trailingParens > 0
  const attrs = parseAttrs(done ? attrsRaw.slice(0, -trailingParens) : attrsRaw)
  if (done) {
    // console.log('done', name)
    i = nl - trailingParens + 1 // skip just one trailing paren
    return {expr: {name, attrs, children: []}, i}
  } else {
    i = nl + 1 // skip the newline
    let res = parseChildren(text, i)
    // console.log('done w/ children', name)
    return {expr: {name, attrs, children: res.children}, i: res.i + 1 /* for the ) */}
  }
}

const parse = text => {
  return parseExpr(text, 0)
}

const fs = require('fs')
const path = require('path')

const walk = (dir, fn) => {
  const files = fs.readdirSync(dir)
  files.forEach(name => {
    const full = path.join(dir, name)
    if (fs.statSync(full).isFile()) {
      fn(full)
    } else {
      walk(full, fn)
    }
  })
}

/*
SignalProducer<Bool, NoError> -> () -> SignalProducer<Bool, NoError>

[
    with SignalProducer<Bool, NoError>[
      SignalProducer<Bool, NoError>: specialize <Bool, NoError>
      (<Value, Error> SignalProducer<Value, Error>: SignalProducerType module ReactiveCocoa)
    ],
    NoError[NoError: ErrorType module Result],
    Bool[Bool: Equatable module Swift]
]
*/

walk('./ast', full => {
  if (!full.match(/\.ast$/)) return
  if (fs.existsSync(full + '.json')) return // already processed
  console.log('processing', full)
  const text = fs.readFileSync(full).toString('utf8')
  const data = parse(text)
  fs.writeFileSync(full + '.json', JSON.stringify(data, null, 2))
})

/*
const text = fs.readFileSync('./ast/VideoTimeFormatter.ast').toString('utf8')
const data = parse(text)
fs.writeFileSync('./parsed.json', JSON.stringify(data, null, 2))
console.log(text.length)
*/
