import React from "react";
import {withRouter} from "react-router-dom";
import PropTypes from 'prop-types';
import DeployResource from "../common/deploy-resource";
import Spinner from "../common/spinner";
import ErrorMsg from "../common/error-msg";
import './editor.css';

/**
 * Edits a file using the first editor
 * out of the list with found editors.
 */
class Editor extends React.Component {

    constructor(props) {
        super(props);
        this.state = {
            objectId: this.props.match.params.objectId,
            objectName: this.props.match.params.objectName,
            editorFile: null,
            workDir: null,
            edit: false
        };
        this.getEditorOfObject(this.state);
    }

    async getEditorOfObject() {
        const params = {
            "params": [{
                "name": "input",
                "type": "file",
                "value": this.state.objectId
            }]
        };
        try {
            const data = await DeployResource
                .getEditors(this.state.objectId)
                .catch((e) => this.setState({error: e}));
            if (!Array.isArray(data) || data.length < 1) {
                this.setState({error: Error("No editor found for " + this.state.objectName)});
                return;
            }

            const editor = data[0].name;

            // TODO: uncomment when deployment service works:
            // const deployData = await DeployResource
            //     .postDeployment(editor, params)
            //     .catch((e) => this.setState({error: e}));
            // if(!deployData) return;
            //
            // const editorData = await DeployResource
            //     .getDeploymentWhen(deployData.workDir, DeploymentStatus.FINISHED)
            //     .catch((e) => this.setState({error: e}));
            // if(!editorData) return;
            //
            // this.setState({
            //     editor: editor,
            //     workDir: editorData.workDir,
            //     editorFileContent: {__html: editorData.viewerFileContent}
            // });

            this.setState({
                workDir: "dummy-uuid-workdir",
                editor: editor,
                editorFileContent: {__html: '<iframe src="" width="100%" height="800px">iframe not supported</iframe>'},
            });

        } catch (e) {
            this.setState({error: {message: "Could not edit file: " + e.message}})
        }

    }

    async stopEditor() {
        if(!this.state.workDir) {
            return;
        }
        const deleted = await DeployResource
            .deleteDeployment(this.state.workDir)
            .then(() => true)
            .catch((e) => {
                this.setState({error: e});
            });
        if(deleted) {
            this.props.history.push('/files');
        }
    }

    render() {
        if (this.state.error)
            return <ErrorMsg error={this.state.error}/>;

        const editorFile = this.state.editorFileContent
            ? <div dangerouslySetInnerHTML={this.state.editorFileContent}/>
            : null;

        const spinner = !this.state.editorFileContent && !this.state.error
            ? <Spinner response={this.state.error}/>
            : null;

        const usingEditor = this.state.editor
            ? <span>With editor: <code>{this.state.editor}</code></span>
            : null;

        return (
            <div>
                <button type="button" className="btn-warning btn-lg pull-right close-editor-btn" onClick={() => this.stopEditor()}>
                    <i className="fa fa-window-close" aria-hidden="true"/>
                    &nbsp;
                    <span>Sluit af</span>
                </button>
                <h1>Editing file {this.state.objectName}</h1>
                <ErrorMsg response={this.state.error}/>
                <p>{usingEditor}</p>
                <div>{this.props.content}</div>
                {editorFile}
                {spinner}
            </div>
        );
    }
}

Editor.propTypes = {
    match: PropTypes.shape({
        params: PropTypes.shape({
            objectId: PropTypes.string.isRequired,
            objectName: PropTypes.string.isRequired
        })
    })
};

export default withRouter(Editor);
