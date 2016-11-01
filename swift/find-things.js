var xcode = require('xcode')
var pbxpath = '/Users/jared/khan/iOS/Khan Academy.xcodeproj/project.pbxproj'
global.proj = xcode.project(pbxpath)
proj.parseSync()

global.findThing = (obj, val, path) => {
  if (obj === val) return [path]
  if (!obj) return []
  var res = []
  if (Array.isArray(obj)) {
    obj.forEach((item, i) => {
      var childRes = findThing(item, val, path.concat([i]))
      if (childRes.length) {
        res = res.concat(childRes)
      }
    })
    return res
  }
  if (typeof obj === 'object') {
    for (var name in obj) {
      if (name === val) {
        res.push(path.concat([name]))
      }
      var childRes = findThing(obj[name], val, path.concat([name]))
      if (childRes.length) {
        res = res.concat(childRes)
      }
    }
    return res;
  }
  return []
}

const path = require('path')
const join = path.join

const parentLess = groups => {
  const parents = {}
  const traverse = (key, parent) => {
    if (key.match(/_comment$/)) return
    if (parents[key]) {
      return
    }
    parents[key] = parents[key] || parent
    if (!groups[key] || !groups[key].children) return
    groups[key].children.forEach(child => traverse(child.value, key))
  }
  Object.keys(groups).forEach(k => traverse(k, false))
  return Object.keys(parents).filter(k => parents[k] === false)
}

const cleanPath = path => {
  if (path[0] === '"' && path[path.length - 1] === '"') {
    return JSON.parse(path)
  }
  return path
}

global.format = val => findThing(proj.hash, val, []).map(m =>  'proj.hash["' + m.join('"]["') + '"]').join('\n')

global.figureFiles = objects => {
  const parents = {}
  const all = {}
  const filePaths = {}

  const traverse = (item, path) => {
    if (item.value.match(/_comment$/))return
    const file = objects.PBXFileReference[item.value]
    if (file) {
      const filePath = file.sourceTree === 'SOURCE_ROOT' ? cleanPath(file.path) : join(path, cleanPath(file.path))
      filePaths[item.value] = filePath
      return {
        path: filePath,
      }
    } else {
      const folder = objects.PBXGroup[item.value]
      if (!folder) {
        console.log('no folder', item.value, item)
        return null
      }
      const newPath = folder.path ? cleanPath(folder.path) : path
      return {
        path: newPath,
        children: folder.children.map(child => traverse(child, newPath)),
      }
    }
  }

  parentLess(objects.PBXGroup).forEach(key => {
    const group = objects.PBXGroup[key]
    all[key] = {
      path: group.path || '.',
      children: group.children.map(child => traverse(child, group.path || '.')),
    }
  })

  return filePaths
}

global.filePaths = figureFiles(proj.hash.project.objects)

const fs = require('fs')

const mainTarget = proj.hash["project"]["objects"]["PBXNativeTarget"]["C4BDC9ED1381710C002001D9"]
const buildPhase = proj.hash["project"]["objects"]["PBXSourcesBuildPhase"][mainTarget.buildPhases[1].value]
const goodFilePaths = buildPhase.files.map(file => proj.hash["project"]["objects"]["PBXBuildFile"][file.value]).map(file => filePaths[file.fileRef])

fs.writeFileSync('./main-files.json', JSON.stringify(goodFilePaths, null, 2))



