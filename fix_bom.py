import os

files_to_check = [
    r'app/src/main/res/values/strings.xml',
    r'app/src/main/res/values-in/strings.xml',
    r'app/src/main/res/layout/activity_main.xml',
    r'app/src/main/res/menu/bottom_nav_menu.xml',
    r'app/src/main/res/values/colors.xml'
]

for filepath in files_to_check:
    if os.path.exists(filepath):
        with open(filepath, 'rb') as f:
            content = f.read()
        if content.startswith(b'\xef\xbb\xbf'):
            print(f"Removing BOM from {filepath}")
            with open(filepath, 'wb') as f:
                f.write(content[3:])
        else:
            print(f"No BOM in {filepath}")
