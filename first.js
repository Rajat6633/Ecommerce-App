// let num = "Rajat"
// console.log(num)
// console.log(typeof num)

// // Type convrsion
// let num1 = String(6)
// let num2 = Number(6)
// console.log(typeof num1)
// console.log(typeof num2)

// // Type Coercion, if we add two diff datatype like number and String
// let x 
// console.log(x, typeof x)
// x = x + ""
// console.log(x, typeof x)
// x = x -2
// console.log(x, typeof x)


// Template Literal
// let add1 = 9
// let add2 = 4
// let result = add1 + add2
// console.log("Addition of "+add1 +" and "+add2+" is "+result)

// // While Loop
// let val = 3
// while(val>0)
// {
//     console.log(val)
//     val --
// }

// // For Loop
// let forLoopVal = 3
// console.log("For Loop")
// for(i = 0; i< 3; i++){
//     console.log(i);
// }

// Objects key value pair
console.log("Object")
let obj = {
name:"Rajat",
age: 33,
laptop: {
    cpu:12,
    ram:"16"
}
}
// console.log(obj.laptop?.ram?.length)
// console.log(obj.name)
// console.log(obj.age)
// console.log(obj['age'])
// obj.id = 12
// delete obj.laptop.cpu
// console.log(obj.laptop)

// // Forin loop for iterating object
// for(let key in obj){
//     console.log(key, obj[key])
// }
// for(let key of obj){
//     console.log(key)
// }

// //functions
// let res = function fun() {
//     console.log("res called")
// }
// let res1 = function(num1, num2=1) {
//     console.log(num1 , num2)
// }
// function fun() {
//     console.log("func called")
// }
// res()
// res1(11)
// fun()

// // Arrow Funtion
// let arrowRes = (num1) => {
//     console.log("Arrow", num1);
// };

// arrowRes(1)

// // Arrays

// let array = [
// {
//     name:"Raj",
//     age:"22"
// },{name: " jain"}
// ]
// let b = {
//     name: "RR"
// }
// const itemToRemove = {name:"RR"}
// array.push(b)
// // splice method is used to add or delte from array
// array.splice(array.findIndex(a => a.name === itemToRemove.name),1)
// console.log(array)
// // To iterare array use for of loop
// for(let v of array){
//     console.log(v)
// }

// // Foreach loop
// let arr = [2,3,4,5,6]
// arr.forEach((n, i, arr) => {
// console.log(n, i)
// })

// // Filter Method
// let arr = [2,3,4,5,6]
// arr.filter(v => v%2==0).forEach( n => console.log(n))

// // Set and Map
// let set = new Set()
// set.add(1)
// set.add(1)
// set.add(2)
// set.forEach(v => console.log(v))
//  let map = new Map()
//  map.set(1, "Raj")
//  map.set(2, "Jsin")
// console.log(map.keys())
// console.log(map.get(2))
// console.log(map.has(2))
// for(let [k,v] of map) {
//     console.log(k,": ", v)
// }

// map.forEach((v,k) => {
//     console.log(v,": ", k)
// })