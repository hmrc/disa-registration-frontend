document.addEventListener('DOMContentLoaded', function () {
  const printLinks = document.querySelectorAll('.app-print-link')

  printLinks.forEach(function (link) {
    link.addEventListener('click', function (event) {
      event.preventDefault()
      window.print()
    })
  })
})