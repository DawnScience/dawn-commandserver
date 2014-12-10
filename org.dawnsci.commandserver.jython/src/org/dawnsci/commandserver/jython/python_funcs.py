'''
Shared py/jython functions which for use in consumer interpreter instances
'''

#Borrowed from http://blog.datasingularity.com/?p=134
def __dlsCleanup():
	"""Clear's all non-standard global values from namespace"""
	for uniquevar in [var for var in globals().copy() if var[0] != "_" and var != 'dlsCleanup']:
		del globals()[uniquevar]