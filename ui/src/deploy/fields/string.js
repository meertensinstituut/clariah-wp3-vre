import React from "react";
import {FormControl, FormGroup} from 'react-bootstrap';
import RemoveButton from "./remove-button";

export default class String extends React.Component {

    handleValidation() {
        const length = this.props.value.length;
        if (length === 0) return null;
        return 'success';
    }

    handleChange = (e) => {
        this.props.onChange(e.target.value);
        this.setState({value: e.target.value});
    };

    handleRemove = () => () => {
        this.props.onRemove();
    };

    render() {
        return (
            <FormGroup
                controlId="formBasicText"
                validationState={this.handleValidation()}
            >
                <div className="input-group mb-3">
                    <FormControl
                        type="text"
                        value={this.props.value}
                        onChange={this.handleChange}
                    />
                    <FormControl.Feedback/>
                    <div className="input-group-append">
                        <RemoveButton
                            canRemove={this.props.canRemove}
                            onRemove={this.props.onRemove}
                            nextToInput={true}
                        />
                    </div>
                </div>
            </FormGroup>
        );
    }
}